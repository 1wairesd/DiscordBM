package com.wairesd.discordbm.velocity.models.request;

import java.util.Map;

public record RequestMessage(String type, String command, Map<String, String> options, String requestId) {}