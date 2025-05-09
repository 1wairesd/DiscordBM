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
import java.util.concurrent.CompletableFuture;

/**
 * Represents a Netty-based client used to establish and manage a connection to a Velocity server.
 * This class handles the lifecycle of the client including connecting, sending messages,
 * receiving messages, and handling reconnection logic.
 */
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

    public void close() {
        closing = true;
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
        if (Settings.isDebugConnections()) {
            logger.info("Netty client connection closed");
        }
    }

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
                secretCode
        );
        String json = gson.toJson(registerMsg);
        send(json);
        if (Settings.isDebugCommandRegistrations()) {
            logger.info("Sent initial registration message with no commands.");
        }
    }

    private void scheduleReconnect() {
        if (!closing) {
            if (Settings.isDebugConnections()) {
                logger.info("Scheduling reconnect in 1 second...");
            }
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::connect, 20L);
        }
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}