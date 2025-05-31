package com.wairesd.discordbm.velocity.commandbuilder.validator;

import java.util.Map;

public class SendMessageValidator {
    public static void validate(Map<String, Object> properties) {
        boolean hasMessage = properties.containsKey("message") && !((String) properties.get("message")).isEmpty();
        boolean hasEmbed = properties.containsKey("embed");

        if (!hasMessage && !hasEmbed) {
            throw new IllegalArgumentException("Message or embed is required for SendMessageAction");
        }

        String responseType = (String) properties.getOrDefault("response_type", "REPLY");
        String targetId = (String) properties.get("target_id");
        if ((responseType.equalsIgnoreCase("SPECIFIC_CHANNEL") || responseType.equalsIgnoreCase("EDIT_MESSAGE"))
                && (targetId == null || targetId.isEmpty())) {
            throw new IllegalArgumentException("target_id is required for " + responseType);
        }
    }
}
