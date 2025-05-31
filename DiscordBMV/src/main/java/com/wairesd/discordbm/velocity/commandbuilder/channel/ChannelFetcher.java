package com.wairesd.discordbm.velocity.commandbuilder.channel;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class ChannelFetcher {
    public TextChannel getTextChannel(JDA jda, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found: " + channelId);
        }
        return channel;
    }
}
