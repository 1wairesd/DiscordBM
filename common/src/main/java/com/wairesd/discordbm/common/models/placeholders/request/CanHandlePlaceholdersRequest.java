package com.wairesd.discordbm.common.models.placeholders.request;

import java.util.List;

public record CanHandlePlaceholdersRequest(
        String type,
        String player,
        List<String> placeholders,
        String requestId
) {}