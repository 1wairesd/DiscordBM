package com.wairesd.discordbm.common.models.response;

import com.wairesd.discordbm.common.models.buttons.ButtonDefinition;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;

import java.util.List;

public record ResponseMessage(
        String type,
        String requestId,
        String response,
        EmbedDefinition embed,
        List<ButtonDefinition> buttons
) {}
