package com.wairesd.discordbm.velocity.commandbuilder.registry.buttons;

import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.FormButtonData;

import java.util.concurrent.*;

public class FormButtonRegistry {
    private static final ConcurrentMap<String, FormButtonData> formButtonDataMap = new ConcurrentHashMap<>();

    public void register(String id, String formName, String messageTemplate, String requiredRoleId, long durationMillis) {
        if (id == null || id.isEmpty() || formName == null || formName.isEmpty()) {
            throw new IllegalArgumentException("ID and form_name cannot be null or empty");
        }
        long expirationTime = System.currentTimeMillis() + durationMillis;
        if (expirationTime < 0) {
            expirationTime = Long.MAX_VALUE;
        }

        formButtonDataMap.put(id, new FormButtonData(formName, messageTemplate, requiredRoleId, expirationTime));
    }

    public FormButtonData getFormButtonData(String id) {
        FormButtonData data = formButtonDataMap.remove(id);
        return (data != null && !data.isExpired()) ? data : null;
    }

    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        formButtonDataMap.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
