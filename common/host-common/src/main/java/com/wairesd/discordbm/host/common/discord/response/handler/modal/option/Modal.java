package com.wairesd.discordbm.host.common.discord.response.handler.modal.option;

import com.wairesd.discordbm.common.models.modal.ModalDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.discord.DiscordBMHPlatformManager;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class Modal {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));
    public static DiscordBotListener listener;
    public static DiscordBMHPlatformManager platformManager;

    public Modal(DiscordBotListener listener, DiscordBMHPlatformManager platformManager) {
        this.listener = listener;
        this.platformManager = platformManager;
    }

    public static void handleModalResponse(UUID requestId, ResponseMessage respMsg) {
        ModalDefinition formDef = respMsg.modal();
        if (formDef == null) {
            logger.error("Form definition is null for requestId: {}", requestId);
            return;
        }

        net.dv8tion.jda.api.interactions.modals.Modal.Builder modalBuilder = net.dv8tion.jda.api.interactions.modals.Modal.create(formDef.getCustomId(), formDef.getTitle());

        for (var fieldDef : formDef.getFields()) {
            TextInputStyle style = TextInputStyle.valueOf(fieldDef.getType().toUpperCase());
            TextInput input = TextInput.create(
                            fieldDef.getVariable(),
                            fieldDef.getLabel(),
                            style)
                    .setPlaceholder(fieldDef.getPlaceholder())
                    .setRequired(fieldDef.isRequired())
                    .build();
            modalBuilder.addActionRow(input);
        }

        net.dv8tion.jda.api.interactions.modals.Modal modal = modalBuilder.build();

        String messageTemplate = respMsg.response() != null ? respMsg.response() : "";
        boolean isNettyForm = false;
        if (listener != null) {
            Map<String, String> requestIdToCommand = listener.getRequestIdToCommand();
            if (requestIdToCommand != null && requestIdToCommand.containsKey(requestId.toString())) {
                isNettyForm = true;
            }
        }
        if (!isNettyForm) {
            platformManager.getFormHandlers().put(formDef.getCustomId(), messageTemplate);
        }

        if (respMsg.flags() != null && respMsg.flags().requiresModal()) {
            if (listener != null) {
                listener.formEphemeralMap.put(requestId.toString(), false);
            }
        }

        var event = listener.getRequestSender().getPendingRequests().get(requestId);
        if (event != null) {
            event.replyModal(modal).queue(
                    success -> {
                        if (Settings.isDebugRequestProcessing()) {
                            logger.info("Form sent successfully for requestId: {}", requestId);
                        }
                    },
                    failure -> {
                        logger.error("Failed to send form: {}", failure.getMessage());
                        event.getHook().sendMessage("Failed to open form. Please try again.").setEphemeral(true).queue();
                    }
            );
        } else {
            InteractionHook hook = listener.getRequestSender().removeInteractionHook(requestId);
            if (hook != null) {
                if (respMsg.response() != null && !respMsg.response().isEmpty()) {
                    hook.sendMessage(respMsg.response()).setEphemeral(true).queue();
                }
                hook.sendMessage("Form functionality is not available for deferred responses.").setEphemeral(true).queue();
            } else {
                logger.error("No event or hook found for form requestId: {}", requestId);
            }
        }
    }
}
