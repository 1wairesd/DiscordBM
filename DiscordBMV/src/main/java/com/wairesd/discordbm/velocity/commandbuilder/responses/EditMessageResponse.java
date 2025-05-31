package com.wairesd.discordbm.velocity.commandbuilder.responses;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.strategy.ResponseStrategy;

public class EditMessageResponse implements ResponseStrategy {
    public void apply(Context context, String targetId) {
        context.setMessageIdToEdit(targetId);
    }
}