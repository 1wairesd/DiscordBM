package com.wairesd.discordbm.velocity.commandbuilder.actions.placeholders;

import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.utils.MessageFormatterUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ResolvePlaceholdersAction implements CommandAction {
    private final String template;
    private final String playerTemplate;
    private final DiscordBMV plugin;

    public ResolvePlaceholdersAction(Map<String, Object> properties, DiscordBMV plugin) {
        this.template = (String) properties.get("template");
        this.playerTemplate = (String) properties.get("player");
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) context.getEvent();
        return MessageFormatterUtils.format(playerTemplate, event, context, false)
                .thenCompose(playerName -> {
                    PlaceholdersResolver resolver = new PlaceholdersResolver(plugin);
                    return resolver.resolvePlaceholders(template, playerName, context);
                });
    }
}