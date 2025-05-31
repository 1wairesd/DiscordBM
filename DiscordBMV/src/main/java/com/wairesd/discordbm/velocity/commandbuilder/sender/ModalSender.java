package com.wairesd.discordbm.velocity.commandbuilder.sender;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.concurrent.CompletableFuture;

public class ModalSender {
    public void send(SlashCommandInteractionEvent event, Modal modal, Context context, CompletableFuture<Void> future) {
        try {
            event.replyModal(modal).queue(
                    success -> {},
                    error -> {
                        context.getHook().sendMessage("The form could not be opened. Try again.")
                                .setEphemeral(true)
                                .queue();
                        future.completeExceptionally(error);
                    }
            );
        } catch (IllegalStateException e) {
            context.getHook().sendMessage("The form could not be opened. Try again.")
                    .setEphemeral(true)
                    .queue();
            future.completeExceptionally(e);
        }
    }
}
