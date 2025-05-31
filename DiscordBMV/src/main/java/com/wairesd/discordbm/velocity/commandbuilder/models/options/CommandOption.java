package com.wairesd.discordbm.velocity.commandbuilder.models.options;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public class CommandOption {
    private final String name;
    private final String type;
    private final String description;
    private final boolean required;

    public CommandOption(String name, String type, String description, boolean required) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Option name cannot be null or empty");
        }
        validateType(type);
        this.name = name;
        this.type = type;
        this.description = description != null ? description : "";
        this.required = required;
    }

    private void validateType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Option type cannot be null or empty");
        }
        try {
            OptionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid option type: " + type);
        }
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
}