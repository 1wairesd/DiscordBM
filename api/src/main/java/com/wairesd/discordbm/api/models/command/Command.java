package com.wairesd.discordbm.api.models.command;

import java.util.List;

/**
 * Represents a command that can be registered and executed.
 * This class contains information about the command, such as its name,
 * description, associated plugin name, context, and a list of options.
 */
public class Command {
    public String name;
    public String description;
    public String pluginName;
    public String context;
    public List<CommandOption> options;

    public Command(String name, String description, String pluginName, String context, List<CommandOption> options) {
        this.name = name;
        this.description = description;
        this.pluginName = pluginName;
        this.context = context;
        this.options = options;
    }
}