package com.wairesd.discordbm.velocity.commandbuilder.executor;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.config.configurators.Commands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandResponder {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public void handleFailedValidation(SlashCommandInteractionEvent event, CommandStructured command, Context context) {
        List<CommandAction> failActions = command.getFailActions();
        if (!failActions.isEmpty()) {
            event.deferReply(true).queue(hook -> {
                context.setHook(hook);
                CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                for (CommandAction action : failActions) {
                    chain = chain.thenCompose(v -> action.execute(context));
                }
                chain.thenRun(() -> {
                    if (context.getMessageText() != null && !context.getMessageText().isEmpty()) {
                        hook.sendMessage(context.getMessageText()).setEphemeral(true).queue();
                    }
                }).exceptionally(ex -> {
                    logger.error("Error in fail actions: {}", ex.getMessage(), ex);
                    hook.sendMessage("Error in fail actions.").setEphemeral(true).queue();
                    return null;
                });
            });
        } else {
            event.reply("Command conditions not met").setEphemeral(true).queue();
        }
    }

    public void respond(Context context, SlashCommandInteractionEvent event) {
        switch (context.getResponseType()) {
            case REPLY -> sendReply(context);
            case SPECIFIC_CHANNEL -> sendToChannel(event.getJDA(), context);
            case DIRECT_MESSAGE -> sendDirectMessage(context);
            case EDIT_MESSAGE -> editMessage(event.getChannel(), context);
        }
    }

    public void respondAndCleanup(Context context, SlashCommandInteractionEvent event, InteractionHook hook) {
        respond(context, event);
        switch (context.getResponseType()) {
            case SPECIFIC_CHANNEL, DIRECT_MESSAGE, EDIT_MESSAGE -> hook.deleteOriginal().queue();
        }
    }

    private void sendReply(Context context) {
        InteractionHook hook = context.getHook();
        if (hook == null) {
            logger.warn("Hook is null");
            return;
        }
        var msg = hook.sendMessage(context.replacePlaceholders(context.getMessageText()));
        if (context.getEmbed() != null) msg.setEmbeds(context.getEmbed());
        if (!context.getActionRows().isEmpty()) msg.setComponents(context.getActionRows());
        msg.queue(m -> label(context, m.getChannel().getId(), m.getId()));
    }

    private void sendToChannel(JDA jda, Context context) {
        TextChannel channel = jda.getTextChannelById(context.getTargetChannelId());
        if (channel != null) {
            var msg = channel.sendMessage(context.getMessageText());
            if (context.getEmbed() != null) msg.setEmbeds(context.getEmbed());
            if (!context.getActionRows().isEmpty()) msg.setComponents(context.getActionRows());
            msg.queue(m -> label(context, channel.getId(), m.getId()));
        }
    }

    private void sendDirectMessage(Context context) {
        String userId = context.getTargetUserId();
        if (userId == null) return;
        User user = context.getEvent().getJDA().getUserById(userId);
        if (user != null) {
            user.openPrivateChannel().queue(pc -> {
                var msg = pc.sendMessage(context.getMessageText());
                if (context.getEmbed() != null) msg.setEmbeds(context.getEmbed());
                if (!context.getActionRows().isEmpty()) msg.setComponents(context.getActionRows());
                msg.queue(m -> label(context, pc.getId(), m.getId()));
            });
        }
    }

    private void editMessage(MessageChannelUnion channel, Context context) {
        List<ActionRow> components = context.getActionRows().isEmpty() ? Collections.emptyList() : context.getActionRows();
        channel.editMessageById(context.getMessageIdToEdit(), context.getMessageText())
                .setComponents(components)
                .setEmbeds(context.getEmbed() != null ? List.of(context.getEmbed()) : Collections.emptyList())
                .queue(m -> label(context, channel.getId(), m.getId()));
    }

    private void label(Context context, String channelId, String messageId) {
        String label = context.getExpectedMessageLabel();
        if (label != null) {
            String full = (context.getEvent().getGuild() != null ? context.getEvent().getGuild().getId() : "DM") + "_" + label;
            Commands.plugin.setGlobalMessageLabel(full, channelId, messageId);
        }
    }
}
