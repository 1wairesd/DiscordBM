package com.wairesd.discordbm.host.common.discord.response;

import com.wairesd.discordbm.common.models.buttons.ButtonDefinition;
import com.wairesd.discordbm.common.models.buttons.ButtonStyle;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.form.FormDefinition;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.discord.DiscordBMHPlatformManager;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.request.RequestSender;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.context.Context;
import com.wairesd.discordbm.host.common.commandbuilder.utils.MessageFormatterUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

public class ResponseHandler {
    private static DiscordBotListener listener;
    private static DiscordBMHPlatformManager platformManager;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public static void init(DiscordBotListener discordBotListener, DiscordBMHPlatformManager platform) {
        listener = discordBotListener;
        platformManager = platform;
    }

    public static void handleResponse(ResponseMessage respMsg) {
        if (Settings.isDebugRequestProcessing()) {
            logger.info("Response received for request " + respMsg.requestId() + ": " + respMsg.toString());
        }
        try {
            UUID requestId = UUID.fromString(respMsg.requestId());

            if (respMsg.form() != null) {
                if (respMsg.flags() != null && respMsg.flags().requiresModal()) {
                    handleFormResponse(requestId, respMsg);
                    return;
                }
                handleFormResponse(requestId, respMsg);
                return;
            }

            if (respMsg.flags() != null && respMsg.flags().shouldPreventMessageSend()) {
                if (Settings.isDebugRequestProcessing()) {
                    logger.info("Message sending prevented for requestId: {}", requestId);
                }
                return;
            }

            if (respMsg.flags() != null && respMsg.flags().isFormResponse()) {
                if (Settings.isDebugRequestProcessing()) {
                    logger.info("Ignoring response after modal form for requestId: {}", requestId);
                }
                return;
            }

            InteractionHook buttonHook = (InteractionHook)platformManager.getPendingButtonRequests().remove(requestId);
            if (buttonHook != null) {
                var embedBuilder = new EmbedBuilder();
                if (respMsg.embed() != null) {
                    embedBuilder.setTitle(respMsg.embed().title())
                            .setDescription(respMsg.embed().description())
                            .setColor(new Color(respMsg.embed().color()));
                }
                var embed = embedBuilder.build();

                List<Button> jdaButtons = respMsg.buttons().stream()
                        .map(btn -> Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label()))
                        .collect(Collectors.toList());

                boolean ephemeral = false;
                if (ephemeral) {
                    buttonHook.sendMessageEmbeds(embed).addActionRow(jdaButtons).setEphemeral(true).queue();
                } else {
                    buttonHook.editOriginalEmbeds(embed)
                            .setActionRow(jdaButtons)
                            .queue();
                }
                return;
            }

            InteractionHook storedHook = listener.getRequestSender().removeInteractionHook(requestId);
            if (storedHook != null) {
                if (Settings.isDebugRequestProcessing()) {
                    logger.info("Found stored hook for requestId: {}", requestId);
                }
                sendResponseWithHook(storedHook, respMsg);
                return;
            }

            var event = listener.getRequestSender().getPendingRequests().remove(requestId);
            if (event == null) {
                if (respMsg.embed() != null && respMsg.buttons() != null && !respMsg.buttons().isEmpty()) {
                    if (Settings.isDebugRequestProcessing()) {
                        logger.info("Response after modal form for requestId: {} - ignoring (normal behavior)", requestId);
                    }
                    return;
                }
                
                logger.warn("No event found for requestId: {}, retrying in 100ms", requestId);
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        InteractionHook retryHook = listener.getRequestSender().removeInteractionHook(requestId);
                        if (retryHook != null) {
                            if (Settings.isDebugRequestProcessing()) {
                                logger.info("Found stored hook for requestId: {} on retry", requestId);
                            }
                            sendResponseWithHook(retryHook, respMsg);
                            return;
                        }
                        
                        var retryEvent = listener.getRequestSender().getPendingRequests().remove(requestId);
                        if (retryEvent != null) {
                            if (Settings.isDebugRequestProcessing()) {
                                logger.info("Found event for requestId: {} on retry", requestId);
                            }
                            sendResponse(retryEvent, respMsg);
                        } else {
                            logger.error("Still no event or hook found for requestId: {}", requestId);
                        }
                    }
                }, 100);
                return;
            }
            if (Settings.isDebugRequestProcessing()) {
                logger.info("Found and removed event for requestId: {}", requestId);
            }
            sendResponse(event, respMsg);
        } catch (IllegalArgumentException e) {
            logInvalidUUID(respMsg.requestId(), e);
        }
    }

    private static void handleFormResponse(UUID requestId, ResponseMessage respMsg) {
        FormDefinition formDef = respMsg.form();
        if (formDef == null) {
            logger.error("Form definition is null for requestId: {}", requestId);
            return;
        }

        Modal.Builder modalBuilder = Modal.create(formDef.getCustomId(), formDef.getTitle());
        
        for (var fieldDef : formDef.getFields()) {
            TextInputStyle style = TextInputStyle.valueOf(fieldDef.getType().toUpperCase());
            TextInput input = TextInput.create(
                    fieldDef.getVariable(),
                    fieldDef.getLabel(),
                    style)
                    .setPlaceholder(fieldDef.getPlaceholder())
                    .setRequired(fieldDef.isRequired())
                    .build();
            modalBuilder.addActionRow(input);
        }
        
        Modal modal = modalBuilder.build();

        String messageTemplate = respMsg.response() != null ? respMsg.response() : "";
        platformManager.getFormHandlers().put(formDef.getCustomId(), messageTemplate);

        if (respMsg.flags() != null && respMsg.flags().requiresModal()) {
            if (listener != null) {
                listener.formEphemeralMap.put(requestId.toString(), false);
            }
        }

        var event = listener.getRequestSender().getPendingRequests().remove(requestId);
        if (event != null) {
            event.replyModal(modal).queue(
                    success -> {
                        if (Settings.isDebugRequestProcessing()) {
                            logger.info("Form sent successfully for requestId: {}", requestId);
                        }
                    },
                    failure -> {
                        logger.error("Failed to send form: {}", failure.getMessage());
                        event.getHook().sendMessage("Failed to open form. Please try again.").setEphemeral(true).queue();
                    }
            );
        } else {
            InteractionHook hook = listener.getRequestSender().removeInteractionHook(requestId);
            if (hook != null) {
                if (respMsg.response() != null && !respMsg.response().isEmpty()) {
                    hook.sendMessage(respMsg.response()).setEphemeral(true).queue();
                }
                hook.sendMessage("Form functionality is not available for deferred responses.").setEphemeral(true).queue();
            } else {
                logger.error("No event or hook found for form requestId: {}", requestId);
            }
        }
    }

    private static void sendResponse(SlashCommandInteractionEvent event, ResponseMessage respMsg) {
        boolean ephemeral = false;
        if (respMsg.embed() != null) {
            sendCustomEmbed(event, respMsg.embed(), respMsg.buttons(), UUID.fromString(respMsg.requestId()), ephemeral);
        } else if (respMsg.response() != null) {
            event.getHook().sendMessage(respMsg.response()).setEphemeral(ephemeral).queue(
                    success -> {
                        if (Settings.isDebugRequestProcessing()) {
                            logger.info("Message sent successfully");
                        }
                    },
                    failure -> logger.error("Failed to send message: {}", failure.getMessage())
            );
            if (Settings.isDebugRequestProcessing()) {
                logger.info("Response sent for requestId: {}", respMsg.requestId());
            }
        } else {
            event.getHook().sendMessage("No response provided.").setEphemeral(ephemeral).queue();
        }
    }

    private static void sendCustomEmbed(SlashCommandInteractionEvent event, EmbedDefinition embedDef, List<ButtonDefinition> buttons, UUID requestId, boolean ephemeral) {
        var embedBuilder = new EmbedBuilder();
        if (embedDef.title() != null) {
            embedBuilder.setTitle(embedDef.title());
        }
        if (embedDef.description() != null) {
            Context context = new Context(event);
            String serverName = listener.getRequestSender().getServerNameForRequest(requestId);
            if (serverName != null) {
                Map<String, String> variables = new HashMap<>();
                variables.put(RequestSender.SERVER_NAME_VAR, serverName);
                context.setVariables(variables);
            }
            String description = embedDef.description();
            try {
                description = MessageFormatterUtils.format(description, event, context, false).get();
            } catch (Exception e) {
                if (Settings.isDebugErrors()) {
                    logger.error("Error formatting embed description: {}", e.getMessage());
                }
            }
            embedBuilder.setDescription(description);
        }
        if (embedDef.color() != null) {
            embedBuilder.setColor(new Color(embedDef.color()));
        }
        if (embedDef.fields() != null) {
            for (var field : embedDef.fields()) {
                Context context = new Context(event);
                String serverName = listener.getRequestSender().getServerNameForRequest(requestId);
                if (serverName != null) {
                    Map<String, String> variables = new HashMap<>();
                    variables.put(RequestSender.SERVER_NAME_VAR, serverName);
                    context.setVariables(variables);
                }
                String fieldName = field.name();
                String fieldValue = field.value();
                try {
                    fieldName = MessageFormatterUtils.format(fieldName, event, context, false).get();
                    fieldValue = MessageFormatterUtils.format(fieldValue, event, context, false).get();
                } catch (Exception e) {
                    if (Settings.isDebugErrors()) {
                        logger.error("Error formatting embed field: {}", e.getMessage());
                    }
                }
                embedBuilder.addField(fieldName, fieldValue, field.inline());
            }
        }
        var embed = embedBuilder.build();
        if (buttons != null && !buttons.isEmpty()) {
            List<Button> jdaButtons = buttons.stream()
                    .map(btn -> {
                        if (btn.style() == ButtonStyle.LINK) {
                            return Button.link(btn.url(), btn.label());
                        } else {
                            return Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label())
                                    .withDisabled(btn.disabled());
                        }
                    })
                    .collect(Collectors.toList());
            if (ephemeral) {
                event.getHook().sendMessageEmbeds(embed).addActionRow(jdaButtons).setEphemeral(true).queue();
            } else {
                event.getHook().editOriginalEmbeds(embed)
                        .setActionRow(jdaButtons.toArray(new Button[0]))
                        .queue();
            }
        } else {
            if (Settings.isDebugRequestProcessing()) {
                logger.info("About to send embed for requestId: {}", requestId);
            }
            if (ephemeral) {
                event.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue(
                        success -> {
                            if (Settings.isDebugRequestProcessing()) {
                                logger.info("Successfully sent embed for requestId: {}", requestId);
                            }
                        },
                        failure -> logger.error("Failed to send embed for requestId: {} - {}", requestId, failure.getMessage())
                );
            } else {
                event.getHook().editOriginalEmbeds(embed).queue(
                        success -> {
                            if (Settings.isDebugRequestProcessing()) {
                                logger.info("Successfully sent embed for requestId: {}", requestId);
                            }
                        },
                        failure -> logger.error("Failed to send embed for requestId: {} - {}", requestId, failure.getMessage())
                );
            }
        }
    }

    private static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle getJdaButtonStyle(ButtonStyle style) {
        return switch (style) {
            case PRIMARY -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY;
            case SECONDARY -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SECONDARY;
            case SUCCESS -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS;
            case DANGER -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.DANGER;
            case LINK -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.LINK;
        };
    }

    private static void sendResponseWithHook(InteractionHook hook, ResponseMessage respMsg) {
        boolean ephemeral = false;
        if (respMsg.embed() != null) {
            var embedBuilder = new EmbedBuilder();
            if (respMsg.embed().title() != null) {
                embedBuilder.setTitle(respMsg.embed().title());
            }
            if (respMsg.embed().description() != null) {
                embedBuilder.setDescription(respMsg.embed().description());
            }
            if (respMsg.embed().color() != null) {
                embedBuilder.setColor(new Color(respMsg.embed().color()));
            }
            if (respMsg.embed().fields() != null) {
                for (var field : respMsg.embed().fields()) {
                    embedBuilder.addField(field.name(), field.value(), field.inline());
                }
            }
            var embed = embedBuilder.build();

            if (respMsg.buttons() != null && !respMsg.buttons().isEmpty()) {
                List<Button> jdaButtons = respMsg.buttons().stream()
                        .map(btn -> {
                            if (btn.style() == ButtonStyle.LINK) {
                                return Button.link(btn.url(), btn.label());
                            } else {
                                return Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label())
                                        .withDisabled(btn.disabled());
                            }
                        })
                        .collect(Collectors.toList());
                if (ephemeral) {
                    hook.sendMessageEmbeds(embed).addActionRow(jdaButtons).setEphemeral(true).queue();
                } else {
                    hook.editOriginalEmbeds(embed)
                            .setActionRow(jdaButtons.toArray(new Button[0]))
                            .queue();
                }
            } else {
                if (ephemeral) {
                    hook.sendMessageEmbeds(embed).setEphemeral(true).queue();
                } else {
                    hook.editOriginalEmbeds(embed).queue();
                }
            }
        } else if (respMsg.response() != null) {
            if (ephemeral) {
                hook.sendMessage(respMsg.response()).setEphemeral(true).queue();
            } else {
                hook.editOriginal(respMsg.response()).queue();
            }
        }
    }

    private static void logInvalidUUID(String requestIdStr, IllegalArgumentException e) {
        logger.error("Invalid UUID format for requestId: {}", requestIdStr, e);
    }

    public static void handleFormOnly(ResponseMessage respMsg) {
        UUID requestId = UUID.fromString(respMsg.requestId());
        handleFormResponse(requestId, respMsg);
    }

    public static void sendDirectMessage(ResponseMessage respMsg) {
        String userId = respMsg.userId();
        if (userId == null) {
            logger.error("No userId provided for direct_message");
            return;
        }
        var jda = platformManager.getDiscordBotManager().getJda();
        var user = jda.getUserById(userId);
        if (user == null) {
            logger.error("User with ID {} not found for direct_message", userId);
            return;
        }
        user.openPrivateChannel().queue(pc -> {
            var msgAction = respMsg.response() != null ? pc.sendMessage(respMsg.response()) : pc.sendMessage("");
            if (respMsg.embed() != null) {
                var embed = toJdaEmbed(respMsg.embed()).build();
                msgAction.setEmbeds(embed);
            }
            if (respMsg.buttons() != null && !respMsg.buttons().isEmpty()) {
                List<Button> jdaButtons = respMsg.buttons().stream()
                        .map(btn -> btn.style() == ButtonStyle.LINK ? Button.link(btn.url(), btn.label()) : Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label()).withDisabled(btn.disabled()))
                        .collect(Collectors.toList());
                msgAction.setActionRow(jdaButtons);
            }
            msgAction.queue();
        });
    }

    public static void sendChannelMessage(ResponseMessage respMsg) {
        String channelId = respMsg.channelId();
        if (channelId == null) {
            logger.error("No channelId provided for channel_message");
            return;
        }
        var jda = platformManager.getDiscordBotManager().getJda();
        var channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            logger.error("Channel with ID {} not found for channel_message", channelId);
            return;
        }
        var msgAction = respMsg.response() != null ? channel.sendMessage(respMsg.response()) : channel.sendMessage("");
        if (respMsg.embed() != null) {
            var embed = toJdaEmbed(respMsg.embed()).build();
            msgAction.setEmbeds(embed);
        }
        if (respMsg.buttons() != null && !respMsg.buttons().isEmpty()) {
            List<Button> jdaButtons = respMsg.buttons().stream()
                    .map(btn -> btn.style() == ButtonStyle.LINK ? Button.link(btn.url(), btn.label()) : Button.of(getJdaButtonStyle(btn.style()), btn.customId(), btn.label()).withDisabled(btn.disabled()))
                    .collect(Collectors.toList());
            msgAction.setActionRow(jdaButtons);
        }
        msgAction.queue();
    }

    private static net.dv8tion.jda.api.EmbedBuilder toJdaEmbed(EmbedDefinition embedDef) {
        var embedBuilder = new EmbedBuilder();
        if (embedDef.title() != null) embedBuilder.setTitle(embedDef.title());
        if (embedDef.description() != null) embedBuilder.setDescription(embedDef.description());
        if (embedDef.color() != null) embedBuilder.setColor(new Color(embedDef.color()));
        if (embedDef.fields() != null) {
            for (var field : embedDef.fields()) {
                embedBuilder.addField(field.name(), field.value(), field.inline());
            }
        }
        return embedBuilder;
    }
}