package com.wairesd.discordbm.bukkit.api.models.register;

import com.wairesd.discordbm.bukkit.models.command.Command;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.google.gson.Gson;

import java.util.List;
import java.util.Optional;

public class CommandRegister {
    private final DiscordBMB plugin;
    private final Gson gson;

    public CommandRegister(DiscordBMB plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void register(Command command) {
        String secret = Settings.getSecretCode();
        if (secret == null || secret.isEmpty()) return;

        RegisterMessage<Command> msg = new RegisterMessage<>(
                "register",
                plugin.getServerName(),
                command.pluginName,
                List.of(command),
                secret,
                Optional.empty()
        );

        plugin.getNettyService().sendNettyMessage(gson.toJson(msg));

        if (Settings.isDebugCommandRegistrations()) {
            plugin.getLogger().info("Sent registration message for command: " + command.name);
        }
    }
}
