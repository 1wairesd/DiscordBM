package com.wairesd.discordbm.velocity.discord.response;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class ResponseHelper {

    public void replyCommandRestrictedToDM(SlashCommandInteractionEvent event) {
        event.reply("This command is only available in direct messages.")
                .setEphemeral(true)
                .queue();
    }

    public void replySelectionTimeout(StringSelectInteractionEvent event) {
        event.reply("Selection timeout expired.")
                .setEphemeral(true)
                .queue();
    }

    public void replyNoServerSelected(StringSelectInteractionEvent event) {
        event.reply("No server selected.")
                .setEphemeral(true)
                .queue();
    }

    public void replyServerNotFound(StringSelectInteractionEvent event) {
        event.reply("Selected server not found.")
                .setEphemeral(true)
                .queue();
    }
}
