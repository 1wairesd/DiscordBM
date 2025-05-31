package com.wairesd.discordbm.api.commandbuilder;

import com.wairesd.discordbm.api.models.command.Command;
import com.wairesd.discordbm.api.models.command.CommandOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandBuilder {
    private String name;
    private String description;
    private String pluginName;
    private String context = "both";
    private List<CommandOption> options = new ArrayList<>();

    public CommandBuilder name(String name) {
        this.name = name.toLowerCase().trim();
        return this;
    }

    public CommandBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder pluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public CommandBuilder context(String context) {
        if (!isValidContext(context)) {
            throw new IllegalArgumentException("Invalid context: " + context + ". Must be 'both', 'dm', or 'server'.");
        }
        this.context = context;
        return this;
    }

    private boolean isValidContext(String context) {
        return context.equals("both") || context.equals("dm") || context.equals("server");
    }

    public CommandBuilder addOption(String name, String type, String description, boolean required) {
        options.add(new CommandOption(name.toLowerCase().trim(), type, description, required));
        return this;
    }

    public Command build() {
        validate();
        return new Command(name, description, pluginName, context, Collections.unmodifiableList(options));
    }

    private void validate() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Command name must be set");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalStateException("Command description must be set");
        }
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalStateException("Plugin name must be set");
        }
    }
}