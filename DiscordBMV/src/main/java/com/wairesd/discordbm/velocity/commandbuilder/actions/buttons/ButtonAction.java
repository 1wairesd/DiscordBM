package com.wairesd.discordbm.velocity.commandbuilder.actions.buttons;

import com.wairesd.discordbm.velocity.commandbuilder.validator.ButtonActionValidator;
import com.wairesd.discordbm.velocity.commandbuilder.factory.buttons.ButtonFactory;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.parser.TimeoutParser;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ButtonAction implements CommandAction {
    private final String label;
    private final ButtonStyle style;
    private final String url;
    private final String message;
    private final String emoji;
    private final boolean disabled;
    private final String customId;
    private final String formName;
    private final String requiredRoleId;
    private final long timeoutMs;

    public ButtonAction(Map<String, Object> props) {
        ButtonActionValidator.validateProps(props);

        this.label = (String) props.getOrDefault("label", "Button");
        this.style = ButtonStyle.valueOf(((String) props.getOrDefault("style", "PRIMARY")).toUpperCase());
        this.url = (String) props.getOrDefault("url", "");
        this.message = (String) props.getOrDefault("message", "");
        this.emoji = (String) props.getOrDefault("emoji", "");
        this.disabled = (boolean) props.getOrDefault("disabled", false);
        this.customId = (String) props.get("id");
        this.formName = (String) props.get("form_name");
        this.requiredRoleId = (String) props.get("required_role");
        this.timeoutMs = TimeoutParser.parseTimeout(props.get("timeout"));
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        return CompletableFuture.runAsync(() -> {
            String buttonId = customId != null ? customId : generateCustomId();
            Button button = ButtonFactory.createButton(style, buttonId, label, url, formName, message, timeoutMs, emoji, disabled);
            button = ButtonFactory.applyEmojiAndDisabledState(button, emoji, disabled);
            context.addActionRow(ActionRow.of(button));
        });
    }

    private String generateCustomId() {
        return "btn-" + UUID.randomUUID();
    }
}
