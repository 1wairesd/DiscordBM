package com.wairesd.discordbm.velocity.commandbuilder.utils;

import com.wairesd.discordbm.velocity.config.configurators.Messages;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {

    public static List<String> extractPlaceholders(String template) {
        List<String> placeholders = new ArrayList<>();
        Pattern pattern = Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group());
        }
        return placeholders;
    }

    public static String substitutePlaceholders(String template, Map<String, String> values) {
        String result = template;
        String offlineMessage = Messages.getMessage("offline-player", null);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            boolean isEmptyOrUnchanged = (value == null) || value.isEmpty() || value.equals(placeholder);
            String replacement = isEmptyOrUnchanged ? (offlineMessage != null ? offlineMessage : Messages.DEFAULT_MESSAGE) : value;
            result = result.replace(placeholder, replacement);
        }
        return result;
    }
}
