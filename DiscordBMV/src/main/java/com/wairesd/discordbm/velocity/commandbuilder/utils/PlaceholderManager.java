package com.wairesd.discordbm.velocity.commandbuilder.utils;

import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.Placeholder;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.interactions.Interaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlaceholderManager {
    private final List<Placeholder> placeholders = new ArrayList<>();

    public void registerPlaceholder(Placeholder placeholder) {
        placeholders.add(placeholder);
    }

    public CompletableFuture<String> applyPlaceholders(String template, Interaction event, Context context) {
        CompletableFuture<String> resultFuture = CompletableFuture.completedFuture(template);
        for (Placeholder placeholder : placeholders) {
            resultFuture = resultFuture.thenCompose(currentTemplate ->
                    placeholder.replace(currentTemplate, event, context));
        }
        return resultFuture;
    }
}