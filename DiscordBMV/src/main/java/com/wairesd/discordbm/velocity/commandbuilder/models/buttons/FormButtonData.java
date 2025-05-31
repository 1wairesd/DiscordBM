package com.wairesd.discordbm.velocity.commandbuilder.models.buttons;

public class FormButtonData {
    private final String formName;
    private final String messageTemplate;
    private final String requiredRoleId;
    private final long expirationTime;

    public FormButtonData(String formName, String messageTemplate, String requiredRoleId, long expirationTime) {
        this.formName = formName;
        this.messageTemplate = messageTemplate;
        this.requiredRoleId = requiredRoleId;
        this.expirationTime = expirationTime;
    }

    public String getFormName() {
        return formName;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public String getRequiredRoleId() {
        return requiredRoleId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTime;
    }
}
