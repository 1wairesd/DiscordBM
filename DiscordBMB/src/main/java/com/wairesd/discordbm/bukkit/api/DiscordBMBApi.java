package com.wairesd.discordbm.bukkit.api;

import com.google.gson.Gson;
import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.bukkit.models.command.Command;
import com.wairesd.discordbm.bukkit.api.models.register.CommandRegister;
import com.wairesd.discordbm.bukkit.api.models.unregister.CommandUnregister;
import com.wairesd.discordbm.bukkit.handler.DiscordCommandHandler;
import com.wairesd.discordbm.bukkit.DiscordBMB.DiscordCommandRegistrationListener;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;

public class DiscordBMBApi {
    private final DiscordBMB plugin;
    private final CommandRegister commandRegister;
    private final CommandUnregister commandUnregister;

    public DiscordBMBApi(DiscordBMB plugin) {
        this.plugin = plugin;
        this.commandRegister = new CommandRegister(plugin);
        this.commandUnregister = new CommandUnregister(plugin);
    }

    public void registerCommand(Command command,
                                DiscordCommandHandler handler,
                                DiscordCommandRegistrationListener listener) {
        plugin.registerCommandHandler(
                command.name, handler, listener, command
        );
        if (plugin.getNettyService().getNettyClient() != null
                && plugin.getNettyService().getNettyClient().isActive()) {
            commandRegister.register(command);
        }
    }

    public void unregisterCommand(String commandName, String pluginName) {
        if (plugin.getNettyService().getNettyClient() != null
                && plugin.getNettyService().getNettyClient().isActive()) {
            commandUnregister.unregister(commandName, pluginName);
        }
    }

    public void sendResponse(String requestId, EmbedDefinition embed) {
        String embedJson = new Gson().toJson(embed);
        plugin.getNettyService().sendResponse(requestId, embedJson);
    }

    public void sendNettyMessage(String message) {
        plugin.getNettyService().sendNettyMessage(message);
    }
}
