package com.wairesd.discordbm.velocity.commandbuilder.models.codinations;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

public interface CommandCondition {
    boolean check(Context context);
}