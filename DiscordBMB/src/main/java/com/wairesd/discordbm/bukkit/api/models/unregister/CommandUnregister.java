package com.wairesd.discordbm.bukkit.api.models.unregister;

import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.bukkit.models.unregister.UnregisterMessage;
import com.google.gson.Gson;

public class CommandUnregister {
    private final DiscordBMB plugin;
    private final Gson gson;

    public CommandUnregister(DiscordBMB plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void unregister(String commandName, String pluginName) {
        String secret = Settings.getSecretCode();
        if (secret == null || secret.isEmpty()) return;

        UnregisterMessage msg = new UnregisterMessage(
                plugin.getServerName(),
                pluginName,
                commandName,
                secret
        );
        plugin.getNettyService().sendNettyMessage(gson.toJson(msg));

        if (Settings.isDebugCommandRegistrations()) {
            plugin.getLogger().info("Sent unregistration message for command: " + commandName);
        }
    }
}
