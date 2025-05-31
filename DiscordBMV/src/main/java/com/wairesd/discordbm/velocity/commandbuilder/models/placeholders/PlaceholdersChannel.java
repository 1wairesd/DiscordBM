package com.wairesd.discordbm.velocity.commandbuilder.models.placeholders;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.interactions.Interaction;

import java.util.concurrent.CompletableFuture;

public class PlaceholdersChannel implements Placeholder {

    @Override
    public CompletableFuture<String> replace(String template, Interaction event, Context context) {
        String result = template
                .replace("{channel_id}", event.getChannel().getId())
                .replace("{channel_name}", event.getChannel().getName())
                .replace("{channel}", "#" + event.getChannel().getName());

        return CompletableFuture.completedFuture(result);
    }

    public static String resolveChannelId(String targetId, Context context) {
        if (targetId == null) return null;
        if (targetId.equals("{channel}")) {
            return context.getEvent().getChannel().getId();
        }
        return targetId;
    }
}
