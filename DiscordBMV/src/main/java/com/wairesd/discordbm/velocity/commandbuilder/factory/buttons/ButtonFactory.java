package com.wairesd.discordbm.velocity.commandbuilder.factory.buttons;

import com.wairesd.discordbm.velocity.commandbuilder.actions.buttons.ButtonActionRegistry;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class ButtonFactory {

    public static Button createButton(ButtonStyle style, String buttonId, String label, String url, String formName, String message, long timeoutMs, String emoji, boolean disabled) {
        ButtonActionRegistry buttonActionRegistry = new ButtonActionRegistry();

        if (style == ButtonStyle.LINK) {
            return Button.link(url, label);
        } else if (formName != null) {
            buttonActionRegistry.registerFormButton(buttonId, formName, message, null, timeoutMs);
            return Button.of(style, buttonId, label);
        } else {
            buttonActionRegistry.registerButton(buttonId, message, timeoutMs);
            return Button.of(style, buttonId, label);
        }
    }

    public static Button applyEmojiAndDisabledState(Button button, String emoji, boolean disabled) {
        if (!emoji.isEmpty()) {
            button = button.withEmoji(Emoji.fromUnicode(emoji));
        }
        if (disabled) {
            button = button.asDisabled();
        }
        return button;
    }
}
