package com.wairesd.discordbm.api.commandbuilder;

import com.google.gson.Gson;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.common.models.unregister.UnregisterMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;

public class CommandUnregister {
    private final Platform platform;
    private final Gson gson;
    private final PluginLogger pluginLogger;

    public CommandUnregister(Platform platform, PluginLogger pluginLogger) {
        this.platform = platform;
        this.gson = new Gson();
        this.pluginLogger = pluginLogger;
    }

    public void unregister(String commandName, String pluginName) {
        String secret = platform.getSecretCode();
        if (secret == null || secret.isEmpty()) return;

        UnregisterMessage msg = new UnregisterMessage(
                platform.getServerName(),
                pluginName,
                commandName,
                secret
        );
        platform.getNettyService().sendNettyMessage(gson.toJson(msg));

        if (platform.isDebugCommandRegistrations()) {
            pluginLogger.info("Sent unregistration message for command: " + commandName);
        }
    }
}