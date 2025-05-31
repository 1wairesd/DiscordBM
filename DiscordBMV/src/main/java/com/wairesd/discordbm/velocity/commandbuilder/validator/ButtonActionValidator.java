package com.wairesd.discordbm.velocity.commandbuilder.validator;

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Map;

public class ButtonActionValidator {

    public static void validateProps(Map<String, Object> props) {
        String label = (String) props.get("label");
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("label is required");
        }

        String style = (String) props.getOrDefault("style", "PRIMARY");
        ButtonStyle buttonStyle = ButtonStyle.valueOf(style.toUpperCase());

        if (buttonStyle == ButtonStyle.LINK) {
            String url = (String) props.get("url");
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("url property is required for LINK button");
            }
        } else if (!props.containsKey("form_name") && ((String) props.getOrDefault("message", "")).isEmpty()) {
            throw new IllegalArgumentException("message or form_name is required for non-LINK button");
        }
    }
}
