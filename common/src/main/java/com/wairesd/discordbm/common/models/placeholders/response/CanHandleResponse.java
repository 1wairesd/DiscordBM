package com.wairesd.discordbm.common.models.placeholders.response;

public record CanHandleResponse(
        String type,
        String requestId,
        boolean canHandle
) {}