package com.wairesd.discordbm.velocity.commandbuilder.models.placeholders;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.interactions.Interaction;

import java.util.concurrent.CompletableFuture;

public class PlaceholdersResolved implements Placeholder {
    @Override
    public CompletableFuture<String> replace(String template, Interaction event, Context context) {
        String result = replaceSync(template, context);
        return CompletableFuture.completedFuture(result);
    }

    public static String replaceSync(String template, Context context) {
        if (template == null) return "";
        String result = template;

        if (context != null && result.contains("{resolved_message}")) {
            String resolved = context.getResolvedMessage();
            result = result.replace("{resolved_message}", resolved != null ? resolved : "");
        }

        if (result.contains("{option:")) {
            int start = result.indexOf("{option:");
            int end = result.indexOf("}", start);
            if (end != -1) {
                String placeholder = result.substring(start + 8, end);
                String[] parts = placeholder.split("\\|");
                String optionName = parts[0];
                String defaultValue = parts.length > 1 ? parts[1] : "";
                String optionValue = context.getOption(optionName);
                String replacement = (optionValue != null && !optionValue.isBlank()) ? optionValue : defaultValue;
                result = result.replace("{option:" + placeholder + "}", replacement);
            }
        }

        return result;
    }
}