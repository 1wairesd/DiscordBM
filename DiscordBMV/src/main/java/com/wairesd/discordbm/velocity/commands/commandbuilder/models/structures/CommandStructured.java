package com.wairesd.discordbm.velocity.commands.commandbuilder.models.structures;

import com.wairesd.discordbm.velocity.commands.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commands.commandbuilder.models.codinations.CommandCondition;
import com.wairesd.discordbm.velocity.commands.commandbuilder.models.options.CommandOption;

import java.util.List;

public class CommandStructured {
    private final String name;
    private final String description;
    private final String context;
    private final List<CommandOption> options;
    private final List<CommandCondition> conditions;
    private final List<CommandAction> actions;
    private final List<PlaceholderConfig> placeholderConfigs;

    public CommandStructured(String name, String description, String context,
                             List<CommandOption> options, List<CommandCondition> conditions,
                             List<CommandAction> actions, List<PlaceholderConfig> placeholderConfigs) {
        validateInputs(name, description, context);
        this.name = name;
        this.description = description;
        this.context = context;
        this.options = options != null ? List.copyOf(options) : List.of();
        this.conditions = conditions != null ? List.copyOf(conditions) : List.of();
        this.actions = actions != null ? List.copyOf(actions) : List.of();
        this.placeholderConfigs = placeholderConfigs != null ? List.copyOf(placeholderConfigs) : List.of(); // Инициализация нового поля
    }

    private void validateInputs(String name, String description, String context) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Command name is required");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Command description is required");
        }
        if (!List.of("both", "dm", "server").contains(context)) {
            throw new IllegalArgumentException("Invalid context: " + context);
        }
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getContext() { return context; }
    public List<CommandOption> getOptions() { return options; }
    public List<CommandCondition> getConditions() { return conditions; }
    public List<CommandAction> getActions() { return actions; }
    public List<PlaceholderConfig> getPlaceholderConfigs() { return placeholderConfigs; } // Геттер для placeholderConfigs

    public static class PlaceholderConfig {
        private final String placeholder;
        private final String playerSource;
        private final String serverSource;

        public PlaceholderConfig(String placeholder, String playerSource, String serverSource) {
            this.placeholder = placeholder;
            this.playerSource = playerSource;
            this.serverSource = serverSource;
        }

        public String getPlaceholder() { return placeholder; }
        public String getPlayerSource() { return playerSource; }
        public String getServerSource() { return serverSource; }
    }
}
