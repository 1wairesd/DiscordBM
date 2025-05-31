package com.wairesd.discordbm.velocity.commandbuilder.registry.buttons;

import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.ButtonData;

import java.util.concurrent.*;

public class ButtonRegistry {
    private static final ConcurrentMap<String, ButtonData> buttonDataMap = new ConcurrentHashMap<>();

    public void register(String id, String message, long durationMillis) {
        if (id == null || id.isEmpty() || message == null) {
            throw new IllegalArgumentException("ID and message cannot be null or empty");
        }
        long expirationTime = System.currentTimeMillis() + durationMillis;
        if (expirationTime < 0) {
            expirationTime = Long.MAX_VALUE;
        }

        buttonDataMap.put(id, new ButtonData(message, expirationTime));
    }

    public String getMessage(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ButtonData data = buttonDataMap.remove(id);
        return (data != null && !data.isExpired()) ? data.getMessage() : null;
    }

    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        buttonDataMap.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
