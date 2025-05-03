package com.wairesd.discordbm.velocity.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.wairesd.discordbm.bukkit.models.unregister.UnregisterMessage;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.database.DatabaseManager;
import com.wairesd.discordbm.velocity.discord.ResponseHandler;
import com.wairesd.discordbm.velocity.models.command.CommandDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
    private final Gson gson = new Gson();
    private final Logger logger;
    private final Object jda;
    private final DatabaseManager dbManager;
    private final NettyServer nettyServer;
    private boolean authenticated = false;

    public NettyServerHandler(NettyServer nettyServer, Logger logger, Object jda, DatabaseManager dbManager) {
        this.nettyServer = nettyServer;
        this.logger = logger;
        this.jda = jda;
        this.dbManager = dbManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        if (Settings.isDebugConnections()) {
            logger.info("Client connected: {}", ctx.channel().remoteAddress());
        }
        dbManager.isBlocked(ip).thenAcceptAsync(isBlocked -> {
            if (isBlocked) {
                if (Settings.isViewConnectedBannedIp()) {
                    logger.warn("Blocked connection attempt from {}", ip);
                }
                ctx.writeAndFlush("Error: IP blocked due to multiple failed attempts");
                ctx.close();
            } else {
                ctx.executor().schedule(() -> {
                    if (!authenticated) {
                        if (Settings.isDebugAuthentication()) {
                            logger.warn("Client {} did not authenticate in time. Closing connection.", ip);
                        }
                        ctx.writeAndFlush("Error: Authentication timeout");
                        dbManager.incrementFailedAttempt(ip);
                        ctx.close();
                    }
                }, 30, java.util.concurrent.TimeUnit.SECONDS);
            }
        }, ctx.executor());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        if (Settings.isDebugClientResponses()) {
            logger.info("Received message from client: {}", msg);
        }

        JsonObject json = gson.fromJson(msg, JsonObject.class);
        String type = json.get("type").getAsString();

        switch (type) {
            case "register" -> {
                RegisterMessage regMsg = gson.fromJson(json, RegisterMessage.class);
                InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                String ip = remoteAddress.getAddress().getHostAddress();
                int port = remoteAddress.getPort();
                handleRegister(ctx, regMsg, ip, port);
            }
            case "unregister" -> {
                UnregisterMessage unregMsg = gson.fromJson(json, UnregisterMessage.class);
                handleUnregister(ctx, unregMsg);
            }
            case "response" -> {
                ResponseMessage responseMsg = gson.fromJson(json, ResponseMessage.class);
                if (responseMsg.containsPlaceholders()) {
                    processPlaceholders(responseMsg);
                }
                handleResponse(responseMsg);
            }
            default -> logger.warn("Unknown message type: {}", type);
        }
    }

    private void processPlaceholders(ResponseMessage message) {
        if (message.placeholders() == null || message.placeholders().isEmpty()) return;

        Map<String, String> results = new HashMap<>();
        message.placeholders().forEach((ph, value) -> {
            String processedValue = ph + "_processed";
            results.put(ph, processedValue);
        });
    }

    private void handleUnregister(ChannelHandlerContext ctx, UnregisterMessage unregMsg) {
        if (unregMsg.secret == null || !unregMsg.secret.equals(Settings.getSecretCode())) {
            ctx.writeAndFlush("Error: Invalid secret code");
            return;
        }

        String serverName = unregMsg.serverName;
        String commandName = unregMsg.commandName;

        List<NettyServer.ServerInfo> servers = nettyServer.getCommandToServers().get(commandName);
        if (servers != null) {
            servers.removeIf(serverInfo -> serverInfo.serverName().equals(serverName));
            if (servers.isEmpty()) {
                nettyServer.getCommandDefinitions().remove(commandName);
            }
        }

        if (Settings.isDebugCommandRegistrations()) {
            logger.info("Unregistered command {} for server {}", commandName, serverName);
        }
    }

    private <T> void handleRegister(ChannelHandlerContext ctx, RegisterMessage<T> regMsg, String ip, int port) {
        if (regMsg.secret() == null || !regMsg.secret().equals(Settings.getSecretCode())) {
            ctx.writeAndFlush("Error: Invalid secret code");
            dbManager.incrementFailedAttempt(ip);
            ctx.close();
            return;
        }

        if (!authenticated) {
            authenticated = true;
            dbManager.resetAttempts(ip);
            if (Settings.isDebugAuthentication()) {
                logger.info("Client {} IP - {} Port - {} authenticated successfully", regMsg.serverName(), ip, port);
            }
        }

        regMsg.playerName().ifPresent(playerName -> {
            UUID playerUUID = UUID.randomUUID();
            try {
                dbManager.savePlayer(playerUUID, playerName);
            } catch (SQLException e) {
                logger.error("Failed to save player to database", e);
            }
        });

        nettyServer.setServerName(ctx.channel(), regMsg.serverName());
        if (regMsg.commands() != null && !regMsg.commands().isEmpty()) {
            if (Settings.isDebugPluginConnections()) {
                logger.info("Plugin {} registered commands for server {}", regMsg.pluginName(), regMsg.serverName());
            }
            com.google.gson.JsonElement commandsElement = gson.toJsonTree(regMsg.commands());
            List<CommandDefinition> commandDefinitions = gson.fromJson(commandsElement, new TypeToken<List<CommandDefinition>>() {}.getType());
            nettyServer.registerCommands(regMsg.serverName(), commandDefinitions, ctx.channel());
        }
    }

    private void handleResponse(ResponseMessage respMsg) {
        if (!authenticated) return;
        ResponseHandler.handleResponse(respMsg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String serverName = nettyServer.getServerName(ctx.channel());
        nettyServer.removeServer(ctx.channel());
        if (Settings.isDebugConnections()) {
            if (serverName != null) {
                logger.info("Connection closed: {} ({})", serverName, ctx.channel().remoteAddress());
            } else {
                logger.info("Connection closed: {}", ctx.channel().remoteAddress());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (Settings.isDebugErrors()) {
            logger.error("Exception in Netty channel: {}", ctx.channel().remoteAddress(), cause);
        }
        ctx.close();
    }
}