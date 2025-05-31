package com.wairesd.discordbm.common.utils.color.transform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorParser {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private ColorParser() {}

    public static Component parse(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        String cleaned = ColorNormalizer.normalize(message);

        try {
            return MINI.deserialize(cleaned);
        } catch (Exception ignored) {}

        if (cleaned.contains("&")) {
            try {
                return AMP.deserialize(cleaned);
            } catch (Exception ignored) {}
        }
        if (cleaned.contains("§")) {
            try {
                return SECTION.deserialize(cleaned);
            } catch (Exception ignored) {}
        }

        return Component.text(cleaned);
    }
}
