package com.wairesd.discordbm.host.common.models.request;

import java.util.Map;

public record RequestMessage(String type, String command, Map<String, String> options, String requestId) {}