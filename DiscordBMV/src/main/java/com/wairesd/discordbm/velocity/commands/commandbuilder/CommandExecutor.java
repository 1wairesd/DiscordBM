package com.wairesd.discordbm.velocity.commands.commandbuilder;

import com.wairesd.discordbm.velocity.commands.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commands.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.placeholders.PlaceholderManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private final PlaceholderManager placeholderManager;

    public CommandExecutor(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    public void execute(SlashCommandInteractionEvent event, CommandStructured command) {
        if (event == null || command == null) {
            throw new IllegalArgumentException("Event and command cannot be null");
        }

        Context context = new Context(event);

        processPlaceholders(event, context, command);

        event.deferReply(true).queue(hook -> {
            if (!command.getConditions().stream().allMatch(condition -> condition.check(context))) {
                hook.sendMessage("You don't meet the conditions to use this command.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            command.getActions().forEach(action -> action.execute(context));

            switch (context.getResponseType()) {
                case REPLY -> sendReply(hook, context);
                case SPECIFIC_CHANNEL -> sendToChannel(event.getJDA(), context);
                case DIRECT_MESSAGE, EDIT_MESSAGE -> {
                    sendDirectMessage(context);
                    hook.deleteOriginal().queue();
                }
            }
        });
    }

    private void processPlaceholders(SlashCommandInteractionEvent event, Context context, CommandStructured command) {
        String messageText = context.getMessageText();

        String playerName = event.getUser().getName();
        String serverName = context.getServer();

        String processedText = placeholderManager.resolvePlaceholders(messageText, playerName, serverName).join();

        context.setMessageText(processedText);
    }

    private void sendReply(InteractionHook hook, Context context) {
        var messageText = context.getMessageText().isEmpty() ? " " : context.getMessageText();
        var messageAction = hook.sendMessage(messageText);
        if (!context.getButtons().isEmpty()) {
            messageAction.addActionRow(context.getButtons());
        }
        messageAction.queue();
    }

    private void sendToChannel(JDA jda, Context context) {
        TextChannel channel = jda.getTextChannelById(context.getTargetChannelId());
        if (channel != null) {
            var messageAction = channel.sendMessage(context.getMessageText());
            if (!context.getButtons().isEmpty()) {
                messageAction.addActionRow(context.getButtons());
            }
            messageAction.queue();
        } else {
            logger.warn("Target channel not found for ID: {}", context.getTargetChannelId());
        }
    }

    private void sendDirectMessage(Context context) {
        String userId = context.getTargetUserId();
        if (userId == null || userId.isEmpty()) {
            logger.debug("Attempt to send DM with empty user ID");
            logger.warn("Direct message failed - no target user specified");
            return;
        }

        if (Settings.isDebugCommandRegistrations()) {
            logger.debug("Sending DM to user {}: {}", userId, context.getMessageText());
        }

        User user = context.getEvent().getJDA().getUserById(userId);
        if (user != null) {
            user.openPrivateChannel().queue(pc -> {
                var messageAction = pc.sendMessage(context.getMessageText());
                if (!context.getButtons().isEmpty()) {
                    messageAction.addActionRow(context.getButtons());
                }
                messageAction.queue();
            });
        } else {
            logger.warn("Target user not found for ID: {}", userId);
        }
    }

    private void editMessage(MessageChannelUnion channel, Context context) {
        List<LayoutComponent> components = context.getButtons().isEmpty()
                ? null
                : Collections.singletonList(ActionRow.of(context.getButtons()));

        channel.editMessageById(context.getMessageIdToEdit(), context.getMessageText())
                .setComponents(components)
                .queue();
    }
}