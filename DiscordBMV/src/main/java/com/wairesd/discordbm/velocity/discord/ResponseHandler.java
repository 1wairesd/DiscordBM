package com.wairesd.discordbm.velocity.discord;

import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;

import java.awt.*;
import java.util.UUID;

public class ResponseHandler {
    private static DiscordBotListener listener;
    private static Logger logger;

    public static void init(DiscordBotListener discordBotListener, Logger log) {
        listener = discordBotListener;
        logger = log;
    }

    public static void handleResponse(ResponseMessage respMsg) {
        try {
            UUID requestId = UUID.fromString(respMsg.requestId());
            var event = listener.getPendingRequests().remove(requestId);
            if (event == null) {
                logRequestNotFound(requestId);
                return;
            }

            logReceivedResponse(requestId, respMsg);
            if (respMsg.embed() != null) {
                sendCustomEmbed(event, respMsg.embed());
            } else if (respMsg.response() != null) {
                event.getHook().sendMessage(respMsg.response()).queue();
            } else {
                event.getHook().sendMessage("No response provided.").queue();
            }
        } catch (IllegalArgumentException e) {
            logInvalidUUID(respMsg.requestId(), e);
        }
    }

    private static void sendCustomEmbed(SlashCommandInteractionEvent event, EmbedDefinition embedDef) {
        var embedBuilder = new EmbedBuilder();
        if (embedDef.title() != null) {
            embedBuilder.setTitle(embedDef.title());
        }
        if (embedDef.description() != null) {
            embedBuilder.setDescription(embedDef.description());
        }
        if (embedDef.color() != null) {
            embedBuilder.setColor(new Color(embedDef.color()));
        }
        if (embedDef.fields() != null) {
            for (var field : embedDef.fields()) {
                embedBuilder.addField(field.name(), field.value(), field.inline());
            }
        }
        var embed = embedBuilder.build();
        event.getHook().sendMessageEmbeds(embed).queue();
    }

    private static void logRequestNotFound(UUID requestId) {
        if (Settings.isDebugErrors()) {
            logger.warn("Request with ID {} not found.", requestId);
        }
    }

    private static void logReceivedResponse(UUID requestId, ResponseMessage respMsg) {
        if (Settings.isDebugClientResponses()) {
            logger.info("Received response for request {}: {}", requestId, respMsg);
        }
    }

    private static void logInvalidUUID(String requestIdStr, IllegalArgumentException e) {
        if (Settings.isDebugErrors()) {
            logger.error("Invalid UUID in response: {}", requestIdStr, e);
        }
    }
}