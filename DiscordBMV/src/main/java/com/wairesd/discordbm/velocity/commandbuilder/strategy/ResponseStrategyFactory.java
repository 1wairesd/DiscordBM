package com.wairesd.discordbm.velocity.commandbuilder.strategy;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.ResponseType;
import com.wairesd.discordbm.velocity.commandbuilder.responses.DirectMessageResponse;
import com.wairesd.discordbm.velocity.commandbuilder.responses.EditMessageResponse;
import com.wairesd.discordbm.velocity.commandbuilder.responses.ReplyResponse;
import com.wairesd.discordbm.velocity.commandbuilder.responses.SpecificChannelResponse;

public class ResponseStrategyFactory {
    public static ResponseStrategy getStrategy(ResponseType type) {
        return switch (type) {
            case SPECIFIC_CHANNEL -> new SpecificChannelResponse();
            case DIRECT_MESSAGE -> new DirectMessageResponse();
            case EDIT_MESSAGE -> new EditMessageResponse();
            case REPLY -> new ReplyResponse();
        };
    }
}
