package com.wairesd.discordbm.velocity.commandbuilder.strategy;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

public interface ResponseStrategy {
    void apply(Context context, String targetId);
}
