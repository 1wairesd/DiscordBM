package com.wairesd.discordbm.velocity.commandbuilder.parser;

import com.wairesd.discordbm.velocity.config.configurators.Settings;

public class TimeoutParser {

    public static long parseTimeout(Object timeoutObj) {
        if (timeoutObj == null) {
            return Settings.getButtonTimeoutMs();
        }

        if (timeoutObj instanceof String str) {
            if (str.equalsIgnoreCase("infinite")) {
                return Long.MAX_VALUE;
            } else {
                try {
                    return Long.parseLong(str) * 60_000;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid timeout format: " + str);
                }
            }
        } else if (timeoutObj instanceof Number number) {
            return number.longValue() * 60_000;
        }

        throw new IllegalArgumentException("Unsupported timeout value: " + timeoutObj);
    }
}
