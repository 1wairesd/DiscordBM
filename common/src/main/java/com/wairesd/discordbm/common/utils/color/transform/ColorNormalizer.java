package com.wairesd.discordbm.common.utils.color.transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorNormalizer {

    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RAW_HEX_PATTERN = Pattern.compile("(?<!<)#([A-Fa-f0-9]{6})");

    private ColorNormalizer() {}

    public static String normalize(String message) {
        if (message == null) return "";

        Matcher amp = AMP_HEX_PATTERN.matcher(message);
        message = amp.replaceAll("<#$1>");

        Matcher raw = RAW_HEX_PATTERN.matcher(message);
        return raw.replaceAll("<#$1>");
    }
}
