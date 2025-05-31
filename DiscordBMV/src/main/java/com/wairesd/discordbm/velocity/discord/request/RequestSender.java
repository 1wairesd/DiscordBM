package com.wairesd.discordbm.velocity.discord.request;

import com.google.gson.Gson;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.models.request.RequestMessage;
import com.wairesd.discordbm.velocity.network.NettyServer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RequestSender {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final Gson GSON = new Gson();

    private final NettyServer nettyServer;

    private final ConcurrentHashMap<UUID, SlashCommandInteractionEvent> pendingRequests = new ConcurrentHashMap<>();

    public RequestSender(NettyServer nettyServer, PluginLogger logger) {
        this.nettyServer = nettyServer;
    }

    public void sendRequestToServer(SlashCommandInteractionEvent event, NettyServer.ServerInfo serverInfo) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<InteractionHook> deferFuture = new CompletableFuture<>();

        event.deferReply().queue(
                hook -> deferFuture.complete(hook),
                failure -> deferFuture.completeExceptionally(failure)
        );

        deferFuture.thenAccept(hook -> {
            pendingRequests.put(requestId, event);
            logger.info("Added requestId {} to pendingRequests after defer", requestId);
            RequestMessage request = createRequestMessage(event, requestId);
            String json = GSON.toJson(request);
            nettyServer.sendMessage(serverInfo.channel(), json);
            logger.info("Sent request for requestId {}", requestId);
        }).exceptionally(ex -> {
            logger.error("Failed to defer reply for requestId {}: {}", requestId, ex.getMessage());
            return null;
        });
    }

    private UUID generateRequestId() {
        return UUID.randomUUID();
    }

    private RequestMessage createRequestMessage(SlashCommandInteractionEvent event, UUID requestId) {
        Map<String, String> options = event.getOptions().stream()
                .collect(Collectors.toMap(opt -> opt.getName(), opt -> opt.getAsString()));
        return new RequestMessage("request", event.getName(), options, requestId.toString());
    }

    private void logDebug(String message, String serverName) {
        if (com.wairesd.discordbm.velocity.config.configurators.Settings.isDebugClientResponses()) {
            logger.info("Sending request to server {}: {}", serverName, message);
        }
    }

    public ConcurrentHashMap<UUID, SlashCommandInteractionEvent> getPendingRequests() {
        return pendingRequests;
    }
}
