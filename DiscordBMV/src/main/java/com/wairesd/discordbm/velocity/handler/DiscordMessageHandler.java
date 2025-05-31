package com.wairesd.discordbm.velocity.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class DiscordMessageHandler {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final String DISCORD_MESSAGE_CHANNEL = "discord:message";

    public DiscordMessageHandler() {
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (DISCORD_MESSAGE_CHANNEL.equals(event.getIdentifier().getId())) {
            handleDiscordMessage(event);
        }
    }

    private void handleDiscordMessage(PluginMessageEvent event) {
        String message = new String(event.getData(), StandardCharsets.UTF_8);
        logDebugMessage(message);
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    private void logDebugMessage(String message) {
        if (Settings.isDebugPluginConnections()) {
            logger.info("Received message from Bukkit plugin: {}", message);
        }
    }
}
