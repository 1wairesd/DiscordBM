package com.wairesd.discordbm.velocity.discord.response;

import com.wairesd.discordbm.common.models.buttons.ButtonDefinition;
import com.wairesd.discordbm.common.models.buttons.ButtonStyle;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.discord.DiscordBotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResponseHandler {
    private static DiscordBotListener listener;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public static void init(DiscordBotListener discordBotListener) {
        listener = discordBotListener;
    }

    public static void handleResponse(ResponseMessage respMsg) {
        logger.info("Получен ответ для запроса " + respMsg.requestId() + ": " + respMsg.toString());
        try {
            UUID requestId = UUID.fromString(respMsg.requestId());
            var event = listener.getRequestSender().getPendingRequests().remove(requestId);

            if (event == null) {
                logger.warn("No event found for requestId: {}, retrying in 100ms", requestId);
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        var retryEvent = listener.getRequestSender().getPendingRequests().remove(requestId);
                        if (retryEvent != null) {
                            logger.info("Found event for requestId: {} on retry", requestId);
                            sendResponse(retryEvent, respMsg);
                        } else {
                            logger.error("Still no event found for requestId: {}", requestId);
                        }
                    }
                }, 100);
                return;
            }
            logger.info("Found and removed event for requestId: {}", requestId);
            sendResponse(event, respMsg);
        } catch (IllegalArgumentException e) {
            logInvalidUUID(respMsg.requestId(), e);
        }
    }

    private static void sendResponse(SlashCommandInteractionEvent event, ResponseMessage respMsg) {
        if (respMsg.embed() != null) {
            sendCustomEmbed(event, respMsg.embed(), respMsg.buttons(), UUID.fromString(respMsg.requestId()));
        } else if (respMsg.response() != null) {
            event.getHook().sendMessage(respMsg.response()).queue(
                    success -> logger.info("Message sent successfully"),
                    failure -> logger.error("Failed to send message: {}", failure.getMessage())
            );
            logger.info("Response sent for requestId: {}", respMsg.requestId());
        } else {
            event.getHook().sendMessage("No response provided.").queue();
        }
    }

    private static void sendCustomEmbed(SlashCommandInteractionEvent event, EmbedDefinition embedDef, List<ButtonDefinition> buttons, UUID requestId) {
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

        if (buttons != null && !buttons.isEmpty()) {
            List<Button> jdaButtons = buttons.stream()
                    .map(btn -> {
                        if (btn.style() == ButtonStyle.LINK) {
                            return Button.link(btn.url(), btn.label());
                        } else {
                            return Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label())
                                    .withDisabled(btn.disabled());
                        }
                    })
                    .collect(Collectors.toList());

            event.getHook().editOriginalEmbeds(embed)
                    .setActionRow(jdaButtons.toArray(new Button[0]))
                    .queue();
        } else {
            logger.info("About to send embed for requestId: {}", requestId);
            event.getHook().editOriginalEmbeds(embed).queue(
                    success -> logger.info("Successfully sent embed for requestId: {}", requestId),
                    failure -> logger.error("Failed to send embed for requestId: {} - {}", requestId, failure.getMessage())
            );
        }
    }

    private static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle getJdaButtonStyle(ButtonStyle style) {
        return switch (style) {
            case PRIMARY -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY;
            case SECONDARY -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SECONDARY;
            case SUCCESS -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS;
            case DANGER -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.DANGER;
            case LINK -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.LINK;
        };
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