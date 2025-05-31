package com.wairesd.discordbm.velocity.commandbuilder.executor;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class CommandProcessor {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public void process(CommandStructured command, Context context, SlashCommandInteractionEvent event, CommandResponder responder) {
        boolean ephemeral = command.getEphemeral() != null ? command.getEphemeral() : Settings.isDefaultEphemeral();

        if (command.hasFormAction()) {
            executeActions(command.getActions(), context)
                    .thenRun(() -> responder.respond(context, event))
                    .exceptionally(ex -> {
                        logger.error("Error executing command actions: {}", ex.getMessage(), ex);
                        event.reply("An error occurred while executing the command.").setEphemeral(true).queue();
                        return null;
                    });
        } else {
            event.deferReply(ephemeral).queue(hook -> {
                context.setHook(hook);
                executeActions(command.getActions(), context)
                        .thenRun(() -> responder.respondAndCleanup(context, event, hook))
                        .exceptionally(ex -> {
                            logger.error("Error executing command actions: {}", ex.getMessage(), ex);
                            hook.sendMessage("An error occurred while executing the command.").setEphemeral(true).queue();
                            return null;
                        });
            });
        }
    }

    private CompletableFuture<Void> executeActions(Iterable<CommandAction> actions, Context context) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (CommandAction action : actions) {
            chain = chain.thenCompose(v -> action.execute(context));
        }
        return chain;
    }
}
