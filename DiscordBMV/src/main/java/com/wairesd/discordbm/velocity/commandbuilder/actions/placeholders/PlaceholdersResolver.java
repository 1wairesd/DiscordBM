package com.wairesd.discordbm.velocity.commandbuilder.actions.placeholders;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.commandbuilder.channel.ChannelFinder;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.utils.PlaceholderUtils;
import com.wairesd.discordbm.velocity.network.NettyServer;
import io.netty.channel.Channel;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlaceholdersResolver {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private final DiscordBMV plugin;
    private final NettyServer nettyServer;
    private final ChannelFinder channelFinder;
    private final PlaceholderRequestSender requestSender;

    public PlaceholdersResolver(DiscordBMV plugin) {
        this.plugin = plugin;
        this.nettyServer = plugin.getNettyServer();
        this.channelFinder = new ChannelFinder(nettyServer);
        this.requestSender = new PlaceholderRequestSender(nettyServer);
    }

    public CompletableFuture<Void> resolvePlaceholders(String template, String playerName, Context context) {
        if (nettyServer == null) {
            context.setResolvedMessage("NettyServer is not initialized.");
            return CompletableFuture.completedFuture(null);
        }

        var proxy = plugin.getProxy();
        var playerOpt = proxy.getPlayer(playerName);

        List<String> placeholders = PlaceholderUtils.extractPlaceholders(template);

        if (playerOpt.isPresent()) {
            var player = playerOpt.get();
            var serverOpt = player.getCurrentServer();
            if (serverOpt.isEmpty()) {
                context.setResolvedMessage("The player is online, but not connected to the server.");
                return CompletableFuture.completedFuture(null);
            }
            String serverName = serverOpt.get().getServerInfo().getName();
            Channel channel = channelFinder.findChannelForServer(serverName);
            if (channel == null) {
                context.setResolvedMessage("The server is not connected.");
                return CompletableFuture.completedFuture(null);
            }
            return requestSender.sendGetPlaceholdersRequest(channel, playerName, placeholders)
                    .thenAccept(values -> {
                        String resolved = PlaceholderUtils.substitutePlaceholders(template, values);
                        logger.info("Resolved message: {}", resolved);
                        context.setResolvedMessage(resolved);
                    }).exceptionally(ex -> {
                        context.setResolvedMessage("Error getting placeholders: " + ex.getMessage());
                        return null;
                    });
        } else {
            List<Channel> channels = new ArrayList<>(nettyServer.getChannelToServerName().keySet());
            Map<String, CompletableFuture<Boolean>> canHandleFutures = new HashMap<>();

            for (Channel channel : channels) {
                String serverName = nettyServer.getServerName(channel);
                canHandleFutures.put(serverName,
                        requestSender.sendCanHandlePlaceholdersRequest(channel, playerName, placeholders));
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(canHandleFutures.values().toArray(new CompletableFuture[0]));

            return all.orTimeout(5, TimeUnit.SECONDS).thenCompose(v -> {
                List<String> capableServers = new ArrayList<>();
                for (var entry : canHandleFutures.entrySet()) {
                    try {
                        if (entry.getValue().get()) {
                            capableServers.add(entry.getKey());
                        }
                    } catch (Exception ignored) {}
                }
                if (capableServers.isEmpty()) {
                    context.setResolvedMessage("No server can handle the required placeholders.");
                    return CompletableFuture.completedFuture(null);
                }
                String serverName = capableServers.get(0);
                Channel channel = channelFinder.findChannelForServer(serverName);
                if (channel == null) {
                    context.setResolvedMessage("The selected server is not connected.");
                    return CompletableFuture.completedFuture(null);
                }
                return requestSender.sendGetPlaceholdersRequest(channel, playerName, placeholders)
                        .thenAccept(values -> {
                            String resolved = PlaceholderUtils.substitutePlaceholders(template, values);
                            logger.info("Resolved message: {}", resolved);
                            context.setResolvedMessage(resolved);
                        }).exceptionally(ex -> {
                            context.setResolvedMessage("Error getting placeholders: " + ex.getMessage());
                            return null;
                        });
            });
        }
    }
}
