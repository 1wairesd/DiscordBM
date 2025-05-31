package com.wairesd.discordbm.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.config.ConfigManager;
import com.wairesd.discordbm.velocity.config.configurators.Messages;
import net.kyori.adventure.text.Component;

import java.util.stream.Collectors;

public class CommandAdmin implements SimpleCommand {
    private final DiscordBMV plugin;

    public CommandAdmin(DiscordBMV plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Messages.getParsedMessage("usage-admin-command", null));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!source.hasPermission("discordbotmanager.reload")) {
                    source.sendMessage(Messages.getParsedMessage("no-permission", null));
                    return;
                }
                ConfigManager.ConfigureReload();

                if (plugin.getNettyServer() != null) {
                    plugin.updateActivity();
                    plugin.getCommandManager().loadAndRegisterCommands();
                }
                source.sendMessage(Messages.getParsedMessage("reload-success", null));
                break;
            case "commands":
                if (!source.hasPermission("discordbotmanager.commands")) {
                    source.sendMessage(Messages.getParsedMessage("no-permission", null));
                    return;
                }
                var commandToServers = plugin.getNettyServer().getCommandToServers();
                if (commandToServers.isEmpty()) {
                    source.sendMessage(Messages.getParsedMessage("no-commands-registered", null));
                    return;
                }
                for (var entry : commandToServers.entrySet()) {
                    String command = entry.getKey();
                    String serverList = entry.getValue().stream()
                            .map(server -> server.serverName())
                            .collect(Collectors.joining(", "));
                    source.sendMessage(Component.text(command + ": " + serverList));
                }
                break;

            default:
                source.sendMessage(Messages.getParsedMessage("usage-admin-command", null));
        }
    }
}
