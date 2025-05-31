package com.wairesd.discordbm.common.utils.color;

import com.wairesd.discordbm.common.utils.color.transform.BukkitColorTranslator;
import com.wairesd.discordbm.common.utils.color.transform.ColorParser;
import net.kyori.adventure.text.Component;

public final class ColorUtils {

    private static final boolean BUKKIT_AVAILABLE = BukkitColorTranslator.isBukkitPresent();

    private ColorUtils() {}

    public static Component parseComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        return ColorParser.parse(message);
    }

    public static String parseString(String message) {
        if (message == null || message.isEmpty()) return "";

        return BUKKIT_AVAILABLE
                ? BukkitColorTranslator.translate(message)
                : message;
    }
}
