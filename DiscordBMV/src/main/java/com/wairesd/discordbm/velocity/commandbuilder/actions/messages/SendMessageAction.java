package com.wairesd.discordbm.velocity.commandbuilder.actions.messages;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.ResponseType;
import com.wairesd.discordbm.velocity.commandbuilder.strategy.ResponseStrategy;
import com.wairesd.discordbm.velocity.commandbuilder.strategy.ResponseStrategyFactory;
import com.wairesd.discordbm.velocity.commandbuilder.utils.ContextUtils;
import com.wairesd.discordbm.velocity.commandbuilder.utils.EmbedFactoryUtils;
import com.wairesd.discordbm.velocity.commandbuilder.utils.MessageFormatterUtils;
import com.wairesd.discordbm.velocity.commandbuilder.utils.TargetIDResolverUtils;
import com.wairesd.discordbm.velocity.commandbuilder.validator.SendMessageValidator;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SendMessageAction implements CommandAction {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final String DEFAULT_MESSAGE = "";

    private final String messageTemplate;
    private final ResponseType responseType;
    private final String targetId;
    private final Map<String, Object> embedProperties;
    private final String label;

    public SendMessageAction(Map<String, Object> properties) {
        SendMessageValidator.validate(properties);
        this.messageTemplate = (String) properties.getOrDefault("message", DEFAULT_MESSAGE);
        this.embedProperties = (Map<String, Object>) properties.get("embed");
        this.responseType = ResponseType.valueOf(((String) properties.getOrDefault("response_type", "REPLY")).toUpperCase());
        this.targetId = (String) properties.get("target_id");
        this.label = (String) properties.get("label");
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        ContextUtils.validate(context);
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) context.getEvent();

        OptionMapping targetOption = event.getOption("target");
        if (targetOption != null && targetOption.getAsUser() != null) {
            context.setTargetUser(targetOption.getAsUser());
        }

        String formattedTargetId = TargetIDResolverUtils.resolve(event, this.targetId, context);

        return MessageFormatterUtils.format(messageTemplate, event, context, Settings.isDebugSendMessageAction())
                .thenAccept(formattedMessage -> {
                    context.setMessageText(formattedMessage);
                    context.setResponseType(responseType);

                    if (embedProperties != null) {
                        EmbedFactoryUtils.create(embedProperties, event, context)
                                .thenAccept(context::setEmbed);
                    }

                    if (this.label != null) {
                        context.setExpectedMessageLabel(this.label);
                    }

                    ResponseStrategy strategy = ResponseStrategyFactory.getStrategy(responseType);
                    strategy.apply(context, formattedTargetId);
                });
    }
}