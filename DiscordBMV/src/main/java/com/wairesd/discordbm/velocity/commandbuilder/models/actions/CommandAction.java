package com.wairesd.discordbm.velocity.commandbuilder.models.actions;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

import java.util.concurrent.CompletableFuture;

public interface CommandAction {
    CompletableFuture<Void> execute(Context context);
}