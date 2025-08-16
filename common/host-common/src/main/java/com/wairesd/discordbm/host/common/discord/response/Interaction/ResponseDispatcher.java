package com.wairesd.discordbm.host.common.discord.response.Interaction;

import com.wairesd.discordbm.api.message.ResponseType;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.conditions.CommandCondition;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.context.Context;
import com.wairesd.discordbm.host.common.commandbuilder.core.parser.CommandParserCondition;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.discord.response.ResponseHandler;
import com.wairesd.discordbm.host.common.discord.response.ResponseTypeDetector;
import com.wairesd.discordbm.host.common.utils.Components;
import com.wairesd.discordbm.host.common.utils.Error;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseDispatcher {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));
    private static final int RETRY_DELAY_MS = 100;

    public static void handleResponse(ResponseMessage respMsg) {
        logResponseReceived(respMsg);

        ResponseType responseType = determineResponseType(respMsg);

        if (!checkMessageConditions(respMsg)) {
            return;
        }

        try {
            processResponse(respMsg, responseType);
        } catch (IllegalArgumentException e) {
            Error.logInvalidUUID(respMsg.requestId(), e);
        }
    }

    private static void logResponseReceived(ResponseMessage respMsg) {
        if (Settings.isDebugRequestProcessing()) {
            logger.info("Response received for request " + respMsg.requestId() + ": " + respMsg.toString());
        }
    }

    private static ResponseType determineResponseType(ResponseMessage respMsg) {
        ResponseType responseType = ResponseTypeDetector.determineResponseType(respMsg);
        if (Settings.isDebugRequestProcessing()) {
            logger.info("Auto-detected response type: {} for requestId: {}", responseType, respMsg.requestId());
        }
        return responseType;
    }

    private static boolean checkMessageConditions(ResponseMessage respMsg) {
        if (respMsg.conditions() == null || respMsg.conditions().isEmpty()) {
            return true;
        }

        Context context = createContext(respMsg);

        for (var condMap : respMsg.conditions()) {
            if (!evaluateCondition(condMap, context)) {
                return false;
            }
        }

        return true;
    }

    private static Context createContext(ResponseMessage respMsg) {
        var event = ResponseHandler.listener != null ?
                ResponseHandler.listener.getRequestSender().getPendingRequests().get(respMsg.requestId()) : null;
        return event instanceof SlashCommandInteractionEvent ? new Context((SlashCommandInteractionEvent) event) : new Context((SlashCommandInteractionEvent) null);
    }

    private static boolean evaluateCondition(Object condMap, Context context) {
        try {
            Map<String, Object> conditionMap = extractConditionMap(condMap);
            if (conditionMap == null) {
                return false;
            }
            return checkCondition(conditionMap, context);
        } catch (Exception e) {
            logger.error("Failed to parse/check message condition: {}", condMap, e);
            return false;
        }
    }

    private static Map<String, Object> extractConditionMap(Object condMap) {
        if (!(condMap instanceof Map<?, ?> map)) {
            logger.error("Condition is not a Map: {}", condMap);
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> conditionMap = (Map<String, Object>) map;
        return conditionMap;
    }

    private static boolean checkCondition(Map<String, Object> conditionMap, Context context) {
        CommandCondition condition = CommandParserCondition.parseCondition(conditionMap);
        if (!condition.check(context)) {
            logger.info("Message condition not met, skipping send: {}", conditionMap);
            return false;
        }
        return true;
    }


    private static void processResponse(ResponseMessage respMsg, ResponseType responseType) {
        UUID requestId = extractRequestId(respMsg, responseType);

        if (handleSpecialResponseTypes(respMsg, responseType, requestId)) {
            return;
        }

        if (shouldSkipMessageSend(respMsg)) {
            return;
        }

        if (!processButtonHook(respMsg, requestId) &&
                !processStoredHook(respMsg, requestId)) {
            processPendingEvent(respMsg, requestId);
        }
    }

    private static UUID extractRequestId(ResponseMessage respMsg, ResponseType responseType) {
        switch (responseType) {
            case MODAL:
            case REPLY_MODAL:
            case REPLY:
            case EDIT_MESSAGE:
                return UUID.fromString(respMsg.requestId());
            default:
                return null;
        }
    }

    private static boolean handleSpecialResponseTypes(ResponseMessage respMsg, ResponseType responseType, UUID requestId) {
        switch (responseType) {
            case MODAL:
                return handleModalResponse(respMsg);
            case DIRECT:
                ResponseHandler.sendDirectMessage(respMsg);
                return true;
            case CHANNEL:
                ResponseHandler.sendChannelMessage(respMsg);
                return true;
            case EDIT_MESSAGE:
                ResponseHandler.editMessage(respMsg);
                return true;
            case REPLY_MODAL:
                ResponseHandler.handleReplyModal(requestId, respMsg);
                return true;
            case REPLY:
            default:
                return false;
        }
    }

    private static boolean handleModalResponse(ResponseMessage respMsg) {
        if (ResponseHandler.sentFormRequests.putIfAbsent(respMsg.requestId(), true) != null) {
            return true;
        }
        ResponseHandler.handleModalResponse(UUID.fromString(respMsg.requestId()), respMsg);
        return true;
    }

    private static boolean shouldSkipMessageSend(ResponseMessage respMsg) {
        return (respMsg.flags() != null && respMsg.flags().shouldPreventMessageSend()) ||
                (respMsg.flags() != null && respMsg.flags().isFormResponse());
    }

    private static boolean processButtonHook(ResponseMessage respMsg, UUID requestId) {
        InteractionHook buttonHook = (InteractionHook) ResponseHandler.platformManager.getPendingButtonRequests().remove(requestId);
        if (buttonHook == null) {
            return false;
        }

        handleButtonHookResponse(buttonHook, respMsg);
        return true;
    }

    private static void handleButtonHookResponse(InteractionHook buttonHook, ResponseMessage respMsg) {
        var embed = createEmbed(respMsg);
        List<Button> jdaButtons = createJdaButtons(respMsg);

        boolean ephemeral = false;
        if (ephemeral) {
            buttonHook.sendMessageEmbeds(embed).addActionRow(jdaButtons).setEphemeral(true).queue();
        } else {
            buttonHook.editOriginalEmbeds(embed).setActionRow(jdaButtons).queue();
        }
    }

    private static net.dv8tion.jda.api.entities.MessageEmbed createEmbed(ResponseMessage respMsg) {
        var embedBuilder = new EmbedBuilder();
        if (respMsg.embed() != null) {
            embedBuilder.setTitle(respMsg.embed().title())
                    .setDescription(respMsg.embed().description())
                    .setColor(new Color(respMsg.embed().color()));
        }
        return embedBuilder.build();
    }

    private static List<Button> createJdaButtons(ResponseMessage respMsg) {
        return respMsg.buttons().stream()
                .map(btn -> Button.of(Components.getJdaButtonStyle(btn.style()), btn.customId(), btn.label()))
                .collect(Collectors.toList());
    }

    private static boolean processStoredHook(ResponseMessage respMsg, UUID requestId) {
        InteractionHook storedHook = ResponseHandler.listener.getRequestSender().removeInteractionHook(requestId);
        if (storedHook == null) {
            return false;
        }

        ResponseHandler.sendResponseWithHook(storedHook, respMsg);
        return true;
    }

    private static void processPendingEvent(ResponseMessage respMsg, UUID requestId) {
        var event = ResponseHandler.listener.getRequestSender().getPendingRequests().remove(requestId);
        if (event instanceof SlashCommandInteractionEvent) {
            ResponseHandler.SlashCommandInteraction((SlashCommandInteractionEvent) event, respMsg);
        } else {
            handleMissingEvent(respMsg, requestId);
        }
    }

    private static void handleMissingEvent(ResponseMessage respMsg, UUID requestId) {
        if (shouldSkipRetry(respMsg)) {
            return;
        }

        scheduleRetry(respMsg, requestId);
    }

    private static boolean shouldSkipRetry(ResponseMessage respMsg) {
        return respMsg.embed() != null && respMsg.buttons() != null && !respMsg.buttons().isEmpty();
    }

    private static void scheduleRetry(ResponseMessage respMsg, UUID requestId) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                retryEventProcessing(respMsg, requestId);
            }
        }, RETRY_DELAY_MS);
    }

    private static void retryEventProcessing(ResponseMessage respMsg, UUID requestId) {
        InteractionHook retryHook = ResponseHandler.listener.getRequestSender().removeInteractionHook(requestId);
        if (retryHook != null) {
            ResponseHandler.sendResponseWithHook(retryHook, respMsg);
            return;
        }

        var retryEvent = ResponseHandler.listener.getRequestSender().getPendingRequests().remove(requestId);
        if (retryEvent instanceof SlashCommandInteractionEvent) {
            ResponseHandler.SlashCommandInteraction((SlashCommandInteractionEvent) retryEvent, respMsg);
        } else {
            logger.error("Still no event or hook found for requestId: {}", requestId);
        }
    }
}