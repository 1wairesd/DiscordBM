package com.wairesd.discordbm.velocity.commandbuilder.message;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MessageDeleter {
    public void deleteMessage(TextChannel channel, String messageId) {
        channel.deleteMessageById(messageId).queue();
    }
}
