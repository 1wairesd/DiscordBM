package com.wairesd.discordbm.common.utils.color.transform;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class LegacySerializers {

    public static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    public static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private LegacySerializers() {}
}
