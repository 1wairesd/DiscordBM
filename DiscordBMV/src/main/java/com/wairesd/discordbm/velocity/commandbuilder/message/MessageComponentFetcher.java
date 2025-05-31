package com.wairesd.discordbm.velocity.commandbuilder.message;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;
import java.util.function.Consumer;

public class MessageComponentFetcher {
    private final TextChannel channel;
    private final String messageId;

    public MessageComponentFetcher(TextChannel channel, String messageId) {
        this.channel = channel;
        this.messageId = messageId;
    }

    public void fetchAndApply(Consumer<List<ActionRow>> consumer) {
        channel.retrieveMessageById(messageId).queue(
                (Message message) -> consumer.accept(message.getActionRows()),
                throwable -> {}
        );
    }
}
