package com.wairesd.discordbm.bukkit.network;

import com.google.gson.Gson;
import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.models.command.Command;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NettyClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
    private final InetSocketAddress address;
    private final DiscordBMB plugin;
    private EventLoopGroup group;
    private Channel channel;
    private final Gson gson = new Gson();
    private boolean closing = false;

    public NettyClient(InetSocketAddress address, DiscordBMB plugin) {
        this.address = address;
        this.plugin = plugin;
    }

    /**
     * Initiates an asynchronous connection to a remote Velocity server using Netty.
     * This method sets up the necessary configurations such as event loop groups,
     * channel bootstrap options, and pipeline handlers for communication management.
     *
     * The connection process includes:
     * - Configuring a Netty Bootstrap with appropriate pipeline handlers for decoding and encoding frames and strings.
     * - Handling connection success or failure with appropriate logging and behavior.
     * - Sending a registration message upon successful connection.
     * - Scheduling a reconnect attempt if the connection fails or an error occurs during the attempt.
     *
     * Logging is contingent on debug settings, with detailed output controlled by the following flags:
     * - `Settings.isDebugConnections()` for connection-related logs.
     * - `Settings.isDebugErrors()` for error-specific logs.
     *
     * Any errors encountered during the connection attempt will result in scheduling a reconnect task,
     * provided the client is not currently in the process of closing.
     */
    public void connect() {
        CompletableFuture.runAsync(() -> {
            group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            ch.pipeline().addLast("stringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
                            ch.pipeline().addLast("stringEncoder", new StringEncoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast("handler", new MessageHandler(plugin));
                        }
                    });

            try {
                ChannelFuture future = bootstrap.connect(address).sync();
                if (future.isSuccess()) {
                    channel = future.channel();
                    if (Settings.isDebugConnections()) {
                        logger.info("Connected to Velocity at {}:{}", address.getHostString(), address.getPort());
                    }
                    sendRegistrationMessage();
                } else {
                    if (Settings.isDebugConnections()) {
                        logger.warn("Failed to connect to Velocity at {}:{}: {}", address.getHostString(), address.getPort(), future.cause().getMessage());
                    }
                    scheduleReconnect();
                }
            } catch (InterruptedException e) {
                if (Settings.isDebugErrors()) {
                    logger.error("Connection interrupted", e);
                }
                Thread.currentThread().interrupt();
            }
        }).exceptionally(throwable -> {
            if (Settings.isDebugErrors()) {
                logger.error("Error connecting to Velocity: {}", throwable.getMessage());
            }
            scheduleReconnect();
            return null;
        });
    }

    /**
     * Closes the Netty client connection and releases associated resources.
     *
     * This method performs the following steps to safely shut down the client:
     * - Sets the internal state to indicate that the client is closing.
     * - Closes the network channel if it is active.
     * - Gracefully shuts down the event loop group to release Netty resources.
     * - Logs the closure process if debug connections are enabled in the settings.
     *
     * This is a safe exit method intended to ensure proper resource cleanup
     * during the shutdown of the client.
     */
    public void close() {
        closing = true;
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
        if (Settings.isDebugConnections()) {
            logger.info("Netty client connection closed");
        }
    }

    /**
     * Sends a message through the active channel if the client is active.
     * Logging behavior is controlled by specific debug settings.
     *
     * @param message the message to be sent through the channel
     */
    public void send(String message) {
        if (isActive()) {
            channel.writeAndFlush(message);
            if (Settings.isDebugClientResponses()) {
                logger.debug("Sent message: {}", message);
            }
        } else {
            if (Settings.isDebugErrors()) {
                logger.warn("Cannot send message, channel inactive");
            }
        }
    }

    /**
     * Sends the registration message to the active connection using the provided server and plugin data.
     *
     * The registration message contains information such as the server name, plugin name,
     * an empty list of commands, and a secret code for authentication. The message is
     * serialized into JSON format and sent through the active channel.
     *
     * If no secret code is configured in the settings, this method exits without sending
     * the message.
     *
     * Logging behavior:
     * - If debug settings for command registrations (`Settings.isDebugCommandRegistrations()`)
     *   are enabled, an informational log is recorded upon sending the message.
     */
    private void sendRegistrationMessage() {
        String secretCode = Settings.getSecretCode();
        if (secretCode == null || secretCode.isEmpty()) {
            return;
        }

        RegisterMessage<Command> registerMsg = new RegisterMessage<>(
                "register",
                plugin.getServerName(),
                plugin.getName(),
                List.of(),
                secretCode,
                Optional.empty()
        );

        String json = gson.toJson(registerMsg);
        send(json);
        if (Settings.isDebugCommandRegistrations()) {
            logger.info("Sent initial registration message with no commands.");
        }
    }

    /**
     * Schedules a reconnect task to be executed asynchronously after a delay of 1 second
     * if the client is not in the process of closing.
     *
     * This method checks the `closing` flag to determine whether the client is in the process
     * of shutting down. If `closing` is false, a delayed task is scheduled using the server's
     * asynchronous scheduler.
     *
     * The reconnect task invokes the {@code connect()} method to attempt to re-establish a
     * connection with the server.
     *
     * If debug connection logging is enabled (`Settings.isDebugConnections()`), an informational
     * log message is emitted indicating that a reconnect has been scheduled.
     */
    private void scheduleReconnect() {
        if (!closing) {
            if (Settings.isDebugConnections()) {
                logger.info("Scheduling reconnect in 1 second...");
            }
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::connect, 20L);
        }
    }

    /**
     * Checks whether the Netty client's channel is currently active.
     *
     * This method determines the active state of the client by verifying if
     * the underlying Netty channel is not null and is considered active.
     *
     * @return true if the Netty client's channel is non-null and active, false otherwise
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}