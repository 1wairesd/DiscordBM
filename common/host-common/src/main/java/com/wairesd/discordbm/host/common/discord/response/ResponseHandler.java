package com.wairesd.discordbm.host.common.discord.response;

import com.wairesd.discordbm.common.models.buttons.ButtonDefinition;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.discord.DiscordBMHPlatformManager;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.response.Interaction.ResponseDispatcher;
import com.wairesd.discordbm.host.common.discord.response.Interaction.SlashCommandInteraction;
import com.wairesd.discordbm.host.common.discord.response.handler.error.ErrorHandler;
import com.wairesd.discordbm.host.common.discord.response.handler.editor.MessageEditor;
import com.wairesd.discordbm.host.common.discord.response.handler.sender.MessageSender;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.ModalHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseHandler {
    public static DiscordBotListener listener;
    public static DiscordBMHPlatformManager platformManager;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));
    public static final ConcurrentHashMap<String, Boolean> sentFormRequests = new ConcurrentHashMap<>();

    public static void init(DiscordBotListener discordBotListener, DiscordBMHPlatformManager platform) {
        listener = discordBotListener;
        platformManager = platform;
    }

    public static void handleResponse(ResponseMessage respMsg) {
        ResponseDispatcher.handleResponse(respMsg);
    }

    public static void handleModalResponse(UUID requestId, ResponseMessage respMsg) {
        ModalHandler.handleModalResponse(requestId, respMsg);
    }

    public static void SlashCommandInteraction(SlashCommandInteractionEvent event, ResponseMessage respMsg) {
        SlashCommandInteraction.SlashCommandInteraction(event, respMsg);
    }

    public static void sendEmbed(SlashCommandInteractionEvent event, EmbedDefinition embedDef, List<ButtonDefinition> buttons, UUID requestId, boolean ephemeral) {
        MessageSender.sendEmbed(event, embedDef, buttons, requestId, ephemeral);
    }

    public static void sendResponseWithHook(InteractionHook hook, ResponseMessage respMsg) {
        MessageSender.sendResponseWithHook(hook, respMsg);
    }

    public static void handleFormOnly(ResponseMessage respMsg) {
        UUID requestId = UUID.fromString(respMsg.requestId());
        handleModalResponse(requestId, respMsg);
    }

    public static void handleReplyModal(UUID requestId, ResponseMessage respMsg) {
        ModalHandler.handleReplyModal(requestId, respMsg);
    }

    public static void sendDirectMessage(ResponseMessage respMsg) {
        MessageSender.sendDirectMessage(respMsg);
    }

    public static void sendChannelMessage(ResponseMessage respMsg) {
        MessageSender.sendChannelMessage(respMsg);
    }

    public static void editMessage(ResponseMessage respMsg) {
        MessageEditor.editMessage(respMsg);
    }

    public static void editComponent(ResponseMessage respMsg) {
        MessageEditor.editComponent(respMsg);
    }

    public static void deleteMessage(ResponseMessage respMsg) {
        MessageEditor.deleteMessage(respMsg);
    }
    
    public static void handleConditionError(SlashCommandInteractionEvent event, String errorMessage, boolean ephemeral) {
        ErrorHandler.handleConditionError(event, errorMessage, ephemeral);
    }
}