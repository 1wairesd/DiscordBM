package com.wairesd.discordbm.common.models.placeholders.response;

import java.util.Map;

public record PlaceholdersResponse(
        String type,
        String requestId,
        Map<String, String> values
) {}