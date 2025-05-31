package com.wairesd.discordbm.velocity.commandbuilder.handler.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ButtonResponseHandler {
    public void replyNoPermission(ButtonInteractionEvent event) {
        event.reply("You do not have permission to use this button.").setEphemeral(true).queue();
    }

    public void replyNoForm(ButtonInteractionEvent event) {
        event.deferReply(true).queue(hook ->
                hook.sendMessage("No form found.").setEphemeral(true).queue()
        );
    }

    public void replyMessageOrExpired(ButtonInteractionEvent event, String message) {
        event.deferReply(true).queue(hook -> {
            if (message == null) {
                hook.sendMessage("The action has expired or is invalid").setEphemeral(true).queue();
            } else {
                hook.sendMessage(message).setEphemeral(true).queue();
            }
        });
    }
}
