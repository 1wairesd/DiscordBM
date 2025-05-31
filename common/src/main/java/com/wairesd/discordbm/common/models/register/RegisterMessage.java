package com.wairesd.discordbm.common.models.register;

import java.util.List;

public record RegisterMessage<T>(
        String type,
        String serverName,
        String pluginName,
        List<T> commands,
        String secret
) {}
