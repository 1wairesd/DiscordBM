package com.wairesd.discordbm.velocity.network;

import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.database.DatabaseManager;
import com.wairesd.discordbm.velocity.models.command.CommandDefinition;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyServer {
    private final Logger logger;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final Map<String, CommandDefinition> commandDefinitions = new HashMap<>();
    private final Map<String, List<ServerInfo>> commandToServers = new HashMap<>();
    private final Map<Channel, String> channelToServerName = new ConcurrentHashMap<>();
    private JDA jda;
    private final int port = Settings.getNettyPort();
    private final DatabaseManager dbManager;

    public NettyServer(Logger logger, DatabaseManager dbManager) {
        this.logger = logger;
        this.dbManager = dbManager;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public Map<String, List<ServerInfo>> getCommandToServers() { return commandToServers; }

    public List<ServerInfo> getServersForCommand(String command) {
        return commandToServers.getOrDefault(command, new ArrayList<>());
    }

    public Map<String, CommandDefinition> getCommandDefinitions() { return commandDefinitions; }

    public record ServerInfo(String serverName, Channel channel) {}

    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            ch.pipeline().addLast("stringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
                            ch.pipeline().addLast("stringEncoder", new StringEncoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast("handler", new NettyServerHandler(NettyServer.this, logger, jda, dbManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            if (Settings.isDebugConnections()) {
                logger.info("Netty server started on port {}", port);
            }
            serverChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            if (Settings.isDebugErrors()) {
                logger.error("Netty server interrupted", e);
            }
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (Settings.isDebugConnections()) {
            logger.info("Netty server shutdown complete");
        }
    }

    public void sendMessage(Channel channel, String message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    public void registerCommands(String serverName, List<CommandDefinition> commands, Channel channel) {

        if (jda == null) {
            logger.warn("Cannot register commands - JDA is not initialized!");
            return;
        }

        for (var cmd : commands) {
            if (commandDefinitions.containsKey(cmd.name())) {
                CommandDefinition existing = commandDefinitions.get(cmd.name());
                if (!existing.equals(cmd)) {
                    if (Settings.isDebugErrors()) {
                        logger.error("Command {} from server {} has different definition", cmd.name(), serverName);
                    }
                    continue;
                }
            } else {
                commandDefinitions.put(cmd.name(), cmd);

                if (jda != null) {
                    var cmdData = net.dv8tion.jda.api.interactions.commands.build.Commands.slash(cmd.name(), cmd.description());

                    for (var opt : cmd.options()) {
                        cmdData.addOption(
                                net.dv8tion.jda.api.interactions.commands.OptionType.valueOf(opt.type()),
                                opt.name(),
                                opt.description(),
                                opt.required()
                        );
                    }

                    switch (cmd.context()) {
                        case "both", "dm" -> cmdData.setGuildOnly(false);
                        case "server" -> cmdData.setGuildOnly(true);
                        default -> {
                            if (Settings.isDebugErrors()) {
                                logger.warn("Unknown context '{}' for command '{}'. Defaulting to 'both'.", cmd.context(), cmd.name());
                            }
                            cmdData.setGuildOnly(false);
                        }
                    }

                    ((net.dv8tion.jda.api.JDA) jda).upsertCommand(cmdData).queue();

                    if (Settings.isDebugCommandRegistrations()) {
                        logger.info("Registered command: {} with context: {}", cmd.name(), cmd.context());
                    }
                }
            }

            List<ServerInfo> servers = commandToServers.computeIfAbsent(cmd.name(), k -> new ArrayList<>());
            servers.removeIf(serverInfo -> serverInfo.serverName().equals(serverName));
            servers.add(new ServerInfo(serverName, channel));
        }
    }


    public void removeServer(Channel channel) {
        for (var entry : commandToServers.entrySet()) {
            entry.getValue().removeIf(serverInfo -> serverInfo.channel() == channel);
        }
        channelToServerName.remove(channel);
    }

    public void setServerName(Channel channel, String serverName) {
        channelToServerName.put(channel, serverName);
    }

    public String getServerName(Channel channel) {
        return channelToServerName.get(channel);
    }
}