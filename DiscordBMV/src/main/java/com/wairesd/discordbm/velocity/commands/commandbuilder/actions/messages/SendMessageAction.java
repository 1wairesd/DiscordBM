package com.wairesd.discordbm.velocity.commands.commandbuilder.actions.messages;

import com.wairesd.discordbm.velocity.commands.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commands.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commands.commandbuilder.models.contexts.ResponseType;
import com.wairesd.discordbm.velocity.commands.commandbuilder.placeholders.PlaceholderUser;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;

public class SendMessageAction implements CommandAction {
    private static final Logger logger = LoggerFactory.getLogger(SendMessageAction.class);
    private static final String DEFAULT_MESSAGE = "";
    private final String messageTemplate;
    private final ResponseType responseType;
    private final String targetId;

    public SendMessageAction(Map<String, Object> properties) {
        validateProperties(properties);
        this.messageTemplate = (String) properties.getOrDefault("message", DEFAULT_MESSAGE);

        this.responseType = ResponseType.valueOf(
                ((String) properties.getOrDefault("response_type", "REPLY")).toUpperCase()
        );
        this.targetId = (String) properties.get("target_id");

        if (responseType == ResponseType.SPECIFIC_CHANNEL || responseType == ResponseType.EDIT_MESSAGE) {
            if (targetId == null || targetId.isEmpty()) {
                throw new IllegalArgumentException("target_id is required for " + responseType);
            }
        }
    }

    private void validateProperties(Map<String, Object> properties) {
        String message = (String) properties.get("message");
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message property is required for SendMessageAction");
        }
    }

    @Override
    public void execute(Context context) {
        validateContext(context);
        SlashCommandInteractionEvent event = context.getEvent();
        String formattedTargetId = formatMessage(event, this.targetId);
        String formattedMessage = formatMessage(event, messageTemplate);
        context.setMessageText(formattedMessage);

        context.setResponseType(responseType);
        switch (responseType) {
            case SPECIFIC_CHANNEL:
                context.setTargetChannelId(formattedTargetId);
                break;
            case DIRECT_MESSAGE:
                String userId = formattedTargetId;
                if (userId != null && !userId.isEmpty()) {
                    context.setTargetUserId(userId);
                } else {
                    logger.warn("Target user ID is null or empty, unable to send direct message.");
                }
                break;

            case EDIT_MESSAGE:
                context.setMessageIdToEdit(targetId);
                break;
            case REPLY:
                break;
            default:
                logger.warn("Unknown Response Type: {}", responseType);
                break;
        }
    }

    private void validateContext(Context context) {
        if (context == null || context.getEvent() == null) {
            throw new NullPointerException("Context or event cannot be null");
        }
    }

    private String formatMessage(SlashCommandInteractionEvent event, String template) {
        if (template == null) return "";

        String result = PlaceholderUser.replace(template, event);
        if (result == null) result = "";

        for (OptionMapping option : event.getOptions()) {
            String placeholder = "{" + option.getName() + "}";
            result = result.replace(placeholder, option.getAsString());
        }

        return result;
    }
}
