package com.wairesd.discordbm.velocity.commandbuilder.message;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.util.List;

public class MessageUpdater {
    private final TextChannel channel;
    private final String messageId;
    private final List<ActionRow> updatedRows;

    public MessageUpdater(TextChannel channel, String messageId, List<ActionRow> updatedRows) {
        this.channel = channel;
        this.messageId = messageId;
        this.updatedRows = updatedRows;
    }

    public void update() {
        channel.retrieveMessageById(messageId).queue(
                message -> message.editMessageComponents(updatedRows).queue(),
                throwable -> {}
        );
    }
}
