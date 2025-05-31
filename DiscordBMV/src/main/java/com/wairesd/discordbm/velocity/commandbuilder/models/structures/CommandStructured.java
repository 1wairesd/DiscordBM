package com.wairesd.discordbm.velocity.commandbuilder.models.structures;

import com.wairesd.discordbm.velocity.commandbuilder.actions.forms.SendFormAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.codinations.CommandCondition;
import com.wairesd.discordbm.velocity.commandbuilder.models.options.CommandOption;

import java.util.List;

public class CommandStructured {
    private final String name;
    private final String description;
    private final String context;
    private final List<CommandOption> options;
    private final List<CommandCondition> conditions;
    private final List<CommandAction> actions;
    private final List<CommandAction> failActions;
    private final Boolean ephemeral;

    public CommandStructured(String name, String description, String context,
                             List<CommandOption> options, List<CommandCondition> conditions,
                             List<CommandAction> actions, List<CommandAction> failActions, Boolean ephemeral) {
        validateInputs(name, description, context);
        this.name = name;
        this.description = description;
        this.context = context;
        this.options = options != null ? List.copyOf(options) : List.of();
        this.conditions = conditions != null ? List.copyOf(conditions) : List.of();
        this.actions = actions != null ? List.copyOf(actions) : List.of();
        this.failActions = failActions != null ? List.copyOf(failActions) : List.of();
        this.ephemeral = ephemeral;
    }

    public Boolean getEphemeral() {
        return ephemeral;
    }

    public List<CommandAction> getFailActions() {
        return failActions;
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

    public boolean hasFormAction() {
        return actions.stream().anyMatch(action -> action instanceof SendFormAction);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getContext() { return context; }
    public List<CommandOption> getOptions() { return options; }
    public List<CommandCondition> getConditions() { return conditions; }
    public List<CommandAction> getActions() { return actions; }
}