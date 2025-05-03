package com.wairesd.discordbm.velocity.commands.commandbuilder.placeholders;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PlaceholderUser {
    public static String replace(String template, SlashCommandInteractionEvent event) {
        if (template == null) return null;
        return template
                .replace("{user}", event.getUser().getAsTag())
                .replace("{user_id}", event.getUser().getId())
                .replace("{user_name}", event.getUser().getName());
    }
}