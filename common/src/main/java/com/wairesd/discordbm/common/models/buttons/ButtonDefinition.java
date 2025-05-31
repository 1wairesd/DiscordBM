package com.wairesd.discordbm.common.models.buttons;

public record ButtonDefinition(
        String label,
        String customId,
        ButtonStyle style,
        String url,
        boolean disabled
) {}