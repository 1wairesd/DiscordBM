package com.wairesd.discordbm.api.commandbuilder;

import com.google.gson.Gson;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.api.models.command.Command;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;

import java.util.List;

public class CommandRegister {
    private final Platform platform;
    private final Gson gson;
    private final PluginLogger pluginLogger;

    public CommandRegister(Platform platform, PluginLogger pluginLogger) {
        this.platform = platform;
        this.gson = new Gson();
        this.pluginLogger = pluginLogger;
    }

    public void register(Command command) {
        String secret = platform.getSecretCode();
        if (secret == null || secret.isEmpty()) {
            pluginLogger.warn("Cannot register command: secret is empty!");
            return;
        }

        RegisterMessage<Command> msg = new RegisterMessage<>(
                "register",
                platform.getServerName(),
                command.pluginName,
                List.of(command),
                secret
        );
        platform.getNettyService().sendNettyMessage(gson.toJson(msg));

        if (platform.isDebugCommandRegistrations()) {
            pluginLogger.info("Sent registration message for command: " + command.name);
        }
    }
}