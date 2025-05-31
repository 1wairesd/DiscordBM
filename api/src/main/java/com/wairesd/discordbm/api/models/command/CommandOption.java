package com.wairesd.discordbm.api.models.command;

/**
 * Represents an option or parameter for a command.
 * This class is used to define attributes of a command option, including its
 * name, type, description, and whether it is required.
 *
 * The {@code CommandOption} objects are typically part of a command structure,
 * enabling commands to include configurable options.
 */
public class CommandOption {
    public String name;
    public String type;
    public String description;
    public boolean required;

    public CommandOption(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
}