package com.wairesd.discordbm.api.network;

import com.google.gson.Gson;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import java.net.InetSocketAddress;

public class NettyService {
    private final Platform platform;
    private final Gson gson = new Gson();
    private final PluginLogger pluginLogger;
    private NettyClient nettyClient;

    public NettyService(Platform platform, PluginLogger pluginLogger) {
        this.platform = platform;
        this.pluginLogger = pluginLogger;
    }

    public void initializeNettyClient() {
        String host = platform.getVelocityHost();
        int port = platform.getVelocityPort();
        nettyClient = new NettyClient(new InetSocketAddress(host, port), platform, pluginLogger);
        try {
            nettyClient.connect();
        } catch (Exception e) {
            if (platform.isDebugErrors()) {
                pluginLogger.warn("Failed to connect to Velocity Netty server: " + e.getMessage());
            }
        }
    }

    public void closeNettyConnection() {
        if (nettyClient != null && nettyClient.isActive()) {
            nettyClient.close();
            nettyClient = null;
            if (platform.isDebugConnections()) {
                pluginLogger.info("Netty connection closed.");
            }
        }
    }

    public void sendResponse(String requestId, String embedJson) {
        if (nettyClient != null && nettyClient.isActive()) {
            EmbedDefinition embedObj = gson.fromJson(embedJson, EmbedDefinition.class);
            ResponseMessage respMsg = new ResponseMessage("response", requestId, null, embedObj, null);
            nettyClient.send(gson.toJson(respMsg));
        }
    }

    public void sendNettyMessage(String message) {
        if (nettyClient != null && nettyClient.isActive()) {
            nettyClient.send(message);
        } else if (platform.isDebugErrors()) {
            pluginLogger.warn("Netty connection not active. Message not sent: " + message);
        }
    }

    public NettyClient getNettyClient() {
        return nettyClient;
    }
}