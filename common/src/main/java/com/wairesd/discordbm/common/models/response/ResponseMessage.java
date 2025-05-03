package com.wairesd.discordbm.common.models.response;

import com.wairesd.discordbm.common.models.embed.EmbedDefinition;

import java.util.Map;

public record ResponseMessage(
        String type,
        String requestId,
        String response,
        EmbedDefinition embed,
        Map<String, String> placeholders
) {
    public ResponseMessage(String type, String requestId, String response, EmbedDefinition embed) {
        this(type, requestId, response, embed, null);
    }

    public boolean containsPlaceholders() {
        return placeholders != null && !placeholders.isEmpty();
    }
}

