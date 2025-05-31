package com.wairesd.discordbm.velocity.commandbuilder.utils;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersChannel;
import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersResolved;
import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersServer;
import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersUser;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

public class MessageFormatterUtils {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final PlaceholderManager placeholderManager = new PlaceholderManager();

    static {
        placeholderManager.registerPlaceholder(new PlaceholdersUser());
        placeholderManager.registerPlaceholder(new PlaceholdersServer());
        placeholderManager.registerPlaceholder(new PlaceholdersChannel());
        placeholderManager.registerPlaceholder(new PlaceholdersResolved());
    }

    public static CompletableFuture<String> format(String template, Interaction event, Context context, boolean debugLog) {
        return placeholderManager.applyPlaceholders(template != null ? template : "", event, context)
                .thenApply(result -> {
                    if (event instanceof SlashCommandInteractionEvent slashEvent) {
                        for (OptionMapping option : slashEvent.getOptions()) {
                            result = result.replace("{" + option.getName() + "}", option.getAsString());
                        }
                    }
                    if (debugLog) logger.info("Formatted message: {}", result);
                    return result;
                });
    }
}