package com.wairesd.discordbm.host.common.discord.response.handler.sender.option;

import com.wairesd.discordbm.common.models.buttons.ButtonDefinition;
import com.wairesd.discordbm.common.models.buttons.ButtonStyle;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.context.Context;
import com.wairesd.discordbm.host.common.commandbuilder.utils.MessageFormatterUtils;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.request.RequestSender;
import com.wairesd.discordbm.host.common.utils.Components;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Embed {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));
    public static DiscordBotListener listener;

    public Embed(DiscordBotListener listener) {
        this.listener = listener;
    }

    public static void sendEmbed(
            SlashCommandInteractionEvent event,
            EmbedDefinition embedDef,
            List<ButtonDefinition> buttons,
            UUID requestId,
            boolean ephemeral) {
        var embed = buildEmbed(event, embedDef, requestId);
        if (buttons != null && !buttons.isEmpty()) {
            sendEmbedWithButtons(event, embed, buttons, ephemeral);
        } else {
            sendEmbedWithoutButtons(event, embed, requestId, ephemeral);
        }
    }

    private static MessageEmbed buildEmbed(
            SlashCommandInteractionEvent event, EmbedDefinition embedDef, UUID requestId) {
        var embedBuilder = new EmbedBuilder();
        setTitle(embedBuilder, embedDef);
        setDescription(embedBuilder, embedDef, event, requestId);
        setColor(embedBuilder, embedDef);
        setFields(embedBuilder, embedDef, event, requestId);
        return embedBuilder.build();
    }

    private static void setTitle(EmbedBuilder builder, EmbedDefinition embedDef) {
        if (embedDef.title() != null) {
            builder.setTitle(embedDef.title());
        }
    }

    private static void setDescription(
            EmbedBuilder builder, EmbedDefinition embedDef, SlashCommandInteractionEvent event, UUID requestId) {
        if (embedDef.description() == null) {
            return;
        }
        Context context = createContext(event, requestId);
        String description = embedDef.description();
        try {
            description = MessageFormatterUtils.format(description, event, context, false).get();
        } catch (Exception e) {
            if (Settings.isDebugErrors()) {
                logger.error("Error formatting embed description: {}", e.getMessage());
            }
        }
        builder.setDescription(description);
    }

    private static void setColor(EmbedBuilder builder, EmbedDefinition embedDef) {
        if (embedDef.color() != null) {
            builder.setColor(new Color(embedDef.color()));
        }
    }

    private static void setFields(
            EmbedBuilder builder, EmbedDefinition embedDef, SlashCommandInteractionEvent event, UUID requestId) {
        if (embedDef.fields() == null) {
            return;
        }
        for (var field : embedDef.fields()) {
            Context context = createContext(event, requestId);
            String fieldName = field.name();
            String fieldValue = field.value();
            try {
                fieldName = MessageFormatterUtils.format(fieldName, event, context, false).get();
                fieldValue = MessageFormatterUtils.format(fieldValue, event, context, false).get();
            } catch (Exception e) {
                if (Settings.isDebugErrors()) {
                    logger.error("Error formatting embed field: {}", e.getMessage());
                }
            }
            builder.addField(fieldName, fieldValue, field.inline());
        }
    }

    private static Context createContext(SlashCommandInteractionEvent event, UUID requestId) {
        Context context = new Context(event);
        String serverName = listener.getRequestSender().getServerNameForRequest(requestId);
        if (serverName != null) {
            Map<String, String> variables = new HashMap<>();
            variables.put(RequestSender.SERVER_NAME_VAR, serverName);
            context.setVariables(variables);
        }
        return context;
    }

    private static void sendEmbedWithButtons(
            SlashCommandInteractionEvent event,
            MessageEmbed embed,
            List<ButtonDefinition> buttons,
            boolean ephemeral) {
        List<Button> jdaButtons = buttons.stream()
                .map(btn -> {
                    if (btn.style() == ButtonStyle.LINK) {
                        return Button.link(btn.url(), btn.label());
                    } else {
                        return Button.of(Components.getJdaButtonStyle(btn.style()), btn.customId(), btn.label())
                                .withDisabled(btn.disabled());
                    }
                })
                .collect(Collectors.toList());

        if (ephemeral) {
            event.getHook().sendMessageEmbeds(embed).addActionRow(jdaButtons).setEphemeral(true).queue();
        } else {
            event.getHook().editOriginalEmbeds(embed).setActionRow(jdaButtons.toArray(new Button[0])).queue();
        }
    }

    private static void sendEmbedWithoutButtons(
            SlashCommandInteractionEvent event,
            MessageEmbed embed,
            UUID requestId,
            boolean ephemeral) {
        if (Settings.isDebugRequestProcessing()) {
            logger.info("About to send embed for requestId: {}", requestId);
        }

        if (ephemeral) {
            event.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue(
                    success -> logSuccess(requestId),
                    failure -> logger.error("Failed to send embed for requestId: {} - {}", requestId, failure.getMessage())
            );
        } else {
            event.getHook().editOriginalEmbeds(embed).queue(
                    success -> logSuccess(requestId),
                    failure -> logger.error("Failed to send embed for requestId: {} - {}", requestId, failure.getMessage())
            );
        }
    }

    private static void logSuccess(UUID requestId) {
        if (Settings.isDebugRequestProcessing()) {
            logger.info("Successfully sent embed for requestId: {}", requestId);
        }
    }
}
