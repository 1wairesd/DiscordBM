package com.wairesd.discordbm.velocity.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wairesd.discordbm.common.models.unregister.UnregisterMessage;
import com.wairesd.discordbm.common.models.placeholders.response.CanHandleResponse;
import com.wairesd.discordbm.common.models.placeholders.response.PlaceholdersResponse;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.database.DatabaseManager;
import com.wairesd.discordbm.velocity.handler.RegisterHandle;
import com.wairesd.discordbm.velocity.handler.ResponseHandle;
import com.wairesd.discordbm.velocity.handler.UnregisterHandle;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private final Gson gson = new Gson();
    private final Object jda;
    private final DatabaseManager dbManager;
    private final NettyServer nettyServer;
    private boolean authenticated = false;
    private final RegisterHandle registerHandler;
    private final UnregisterHandle unregisterHandler;
    private final ResponseHandle responseHandle;

    public NettyServerHandler(NettyServer nettyServer, Object jda, DatabaseManager dbManager) {
        this.nettyServer = nettyServer;
        this.jda = jda;
        this.dbManager = dbManager;
        this.registerHandler = new RegisterHandle(this, dbManager, nettyServer);
        this.unregisterHandler = new UnregisterHandle(nettyServer);
        this.responseHandle = new ResponseHandle();
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
                }, 30, TimeUnit.SECONDS);
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

        if ("register".equals(type)) {
            RegisterMessage regMsg = gson.fromJson(json, RegisterMessage.class);
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            String ip = remoteAddress.getAddress().getHostAddress();
            int port = remoteAddress.getPort();
            registerHandler.handleRegister(ctx, regMsg, ip, port);
        } else if ("unregister".equals(type)) {
            UnregisterMessage unregMsg = gson.fromJson(json, UnregisterMessage.class);
            unregisterHandler.handleUnregister(ctx, unregMsg);
        } else if ("response".equals(type)) {
            responseHandle.handleResponse(json);
        } else if ("can_handle_response".equals(type)) {
            CanHandleResponse resp = gson.fromJson(json, CanHandleResponse.class);
            CompletableFuture<Boolean> future = nettyServer.getCanHandleFutures().remove(resp.requestId());
            if (future != null) {
                future.complete(resp.canHandle());
            }
        } else if ("placeholders_response".equals(type)) {
            PlaceholdersResponse resp = gson.fromJson(json, PlaceholdersResponse.class);
            CompletableFuture<PlaceholdersResponse> future = nettyServer.getPlaceholderFutures().remove(resp.requestId());
            if (future != null) {
                future.complete(resp);
            }
        } else {
            logger.warn("Unknown message type: {}", type);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String serverName = nettyServer.getServerName(ctx.channel());
        if (serverName != null) {
            logger.info("Channel inactive for server: {}", serverName);
        }
        ctx.executor().schedule(() -> {
            nettyServer.removeServer(ctx.channel());
            if (Settings.isDebugConnections()) {
                if (serverName != null) {
                    logger.info("Removed server: {} due to inactive channel", serverName);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (Settings.isDebugErrors()) {
            logger.error("Exception in Netty channel: {}", ctx.channel().remoteAddress(), cause);
        }
        ctx.close();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean value) {
        this.authenticated = value;
    }
}