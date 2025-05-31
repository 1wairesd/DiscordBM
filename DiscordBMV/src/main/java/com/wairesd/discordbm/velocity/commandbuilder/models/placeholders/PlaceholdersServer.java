package com.wairesd.discordbm.velocity.commandbuilder.models.placeholders;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.Interaction;
import java.util.concurrent.CompletableFuture;

public class PlaceholdersServer implements Placeholder {
    @Override
    public CompletableFuture<String> replace(String template, Interaction event, Context context) {
        Guild guild = event.getGuild();
        if (guild == null) {
            return CompletableFuture.completedFuture(template
                    .replace("{server_members}", "N/A")
                    .replace("{server_icon}", "N/A")
                    .replace("{server_id}", "N/A")
                    .replace("{server_owner}", "N/A")
                    .replace("{server_owner_id}", "N/A"));
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        guild.retrieveOwner().queue(
                ownerMember -> {
                    User owner = ownerMember != null ? ownerMember.getUser() : null;
                    String result = template
                            .replace("{server_members}", String.valueOf(guild.getMemberCount()))
                            .replace("{server_icon}", guild.getIconUrl() != null ? guild.getIconUrl() : "N/A")
                            .replace("{server_id}", guild.getId())
                            .replace("{server_owner}", owner != null ? owner.getAsTag() : "N/A")
                            .replace("{server_owner_id}", owner != null ? owner.getId() : "N/A");
                    future.complete(result);
                },
                throwable -> future.completeExceptionally(throwable)
        );

        return future;
    }
}