package com.wairesd.discordbm.velocity.commandbuilder.message;

import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersResolved;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.config.configurators.Commands;

public class MessageReferenceResolver {
    public record MessageReference(String channelId, String messageId) {}

    public MessageReference resolve(String label, String channelId, String messageId, Context context) {
        if (label != null) {
            if (channelId != null || messageId != null) {
                throw new IllegalArgumentException("Cannot provide both label and channel_id/message_id");
            }

            String fullLabel = context.getEvent().getGuild().getId() + "_" + label;
            String[] parts = Commands.plugin.getMessageReference(fullLabel);
            if (parts == null || parts.length != 2) {
                throw new IllegalArgumentException("No message found for label " + label);
            }

            return new MessageReference(parts[0], parts[1]);
        } else {
            if (channelId == null || messageId == null) {
                throw new IllegalArgumentException("Either label or both channel_id and message_id must be provided");
            }

            String resolvedChannelId = PlaceholdersResolved.replaceSync(channelId, context);
            String resolvedMessageId = PlaceholdersResolved.replaceSync(messageId, context);
            return new MessageReference(resolvedChannelId, resolvedMessageId);
        }
    }
}
