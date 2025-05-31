package com.wairesd.discordbm.api.handle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.common.models.placeholders.request.CanHandlePlaceholdersRequest;
import com.wairesd.discordbm.common.models.placeholders.request.GetPlaceholdersRequest;
import com.wairesd.discordbm.common.models.placeholders.response.CanHandleResponse;
import com.wairesd.discordbm.common.models.placeholders.response.PlaceholdersResponse;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;

public class MessageHandler extends SimpleChannelInboundHandler<String> {
    private final Platform platform;
    private final Gson gson = new Gson();
    private final PluginLogger pluginLogger;

    public MessageHandler(Platform platform, PluginLogger pluginLogger) {
        this.platform = platform;
        this.pluginLogger = pluginLogger;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) {
        if (platform.isDebugClientResponses()) {
            pluginLogger.info("Received message: {}", message);
        }

        if (message.startsWith("Error:")) {
            handleErrorMessage(message, ctx);
            return;
        }

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();

            if ("request".equals(type)) {
                handleRequest(json);
            } else if ("can_handle_placeholders".equals(type)) {
                CanHandlePlaceholdersRequest req = gson.fromJson(json, CanHandlePlaceholdersRequest.class);
                platform.runTaskAsynchronously(() -> {
                    boolean canHandle = platform.checkIfCanHandle(req.player(), req.placeholders());
                    CanHandleResponse resp = new CanHandleResponse(
                            "can_handle_response",
                            req.requestId(),
                            canHandle
                    );
                    ctx.channel().writeAndFlush(gson.toJson(resp));
                });
            } else if ("get_placeholders".equals(type)) {
                GetPlaceholdersRequest req = gson.fromJson(json, GetPlaceholdersRequest.class);
                platform.runTaskAsynchronously(() -> {
                    Map<String, String> values = platform.getPlaceholderValues(req.player(), req.placeholders());
                    PlaceholdersResponse resp = new PlaceholdersResponse(
                            "placeholders_response",
                            req.requestId(),
                            values
                    );
                    ctx.channel().writeAndFlush(gson.toJson(resp));
                });
            } else {
                pluginLogger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            if (platform.isDebugErrors()) {
                pluginLogger.error("Error processing message: {}", message, e);
            }
        }
    }

    private void handleErrorMessage(String message, ChannelHandlerContext ctx) {
        if (platform.isDebugErrors()) {
            pluginLogger.warn("Received error from server: {}", message);
        }
        switch (message) {
            case "Error: Invalid secret code":
            case "Error: No secret code provided":
            case "Error: Authentication timeout":
                ctx.close();
                break;
            default:
                break;
        }
    }

    private void handleRequest(JsonObject json) {
        String command = json.get("command").getAsString();
        String requestId = json.get("requestId").getAsString();

        Map<String, String> options = new HashMap<>();
        if (json.has("options")) {
            JsonObject optionsJson = json.getAsJsonObject("options");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : optionsJson.entrySet()) {
                options.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        DiscordCommandHandler handler = platform.getCommandHandlers().get(command);
        if (handler != null) {
            String[] args = options.values().toArray(new String[0]);
            platform.runTaskAsynchronously(() -> {
                try {
                    handler.handleCommand(command, args, requestId);
                } catch (Exception e) {
                    platform.getNettyService().sendResponse(requestId,
                            "{\"error\":\"Internal server error\"}");
                }
            });
        } else {
            platform.getNettyService().sendResponse(requestId, "{\"error\":\"Command handler not found\"}");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (platform.isDebugErrors()) {
            pluginLogger.error("Connection error: {}", cause.getMessage(), cause);
        }
        ctx.close();
    }
}