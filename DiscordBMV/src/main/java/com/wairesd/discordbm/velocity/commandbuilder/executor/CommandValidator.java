package com.wairesd.discordbm.velocity.commandbuilder.executor;

import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

public class CommandValidator {
    public boolean validateConditions(CommandStructured command, Context context) {
        return command.getConditions().stream().allMatch(c -> c.check(context));
    }
}
