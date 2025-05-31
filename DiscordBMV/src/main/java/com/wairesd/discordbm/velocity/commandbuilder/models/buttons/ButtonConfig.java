package com.wairesd.discordbm.velocity.commandbuilder.models.buttons;

public class ButtonConfig {
    private final String label;
    private final String targetPage;

    public ButtonConfig(String label, String targetPage) {
        this.label = label;
        this.targetPage = targetPage;
    }

    public String getLabel() { return label; }
    public String getTargetPage() { return targetPage; }
}