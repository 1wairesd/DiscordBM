package com.wairesd.discordbm.common.models.register;

import java.util.List;
import java.util.Optional;

public record RegisterMessage<T>(
        String type,
        String serverName,
        String pluginName,
        List<T> commands,
        String secret,
        Optional<String> playerName
) {}
