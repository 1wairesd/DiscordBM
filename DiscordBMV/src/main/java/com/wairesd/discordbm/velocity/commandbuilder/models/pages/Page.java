package com.wairesd.discordbm.velocity.commandbuilder.models.pages;

import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.ButtonConfig;

import java.util.List;
import java.util.Map;

public class Page {
    private final String id;
    private final String content;
    private final Map<String, Object> embedConfig;
    private final List<ButtonConfig> buttons;

    public Page(String id, String content, Map<String, Object> embedConfig, List<ButtonConfig> buttons) {
        this.id = id;
        this.content = content;
        this.embedConfig = embedConfig;
        this.buttons = buttons;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public Map<String, Object> getEmbedConfig() { return embedConfig; }
    public List<ButtonConfig> getButtons() { return buttons; }
}