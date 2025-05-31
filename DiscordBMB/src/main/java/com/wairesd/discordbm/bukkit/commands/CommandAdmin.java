package com.wairesd.discordbm.bukkit.commands;

import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.bukkit.config.configurators.Messages;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * The CommandAdmin class implements the CommandExecutor and TabCompleter interfaces
 * to create a command handler for administrative tasks within the DiscordBMB plugin.
 *
 * This class supports the execution of specific commands, such as reloading the plugin's
 * configuration. It also provides tab completion suggestions for command arguments.
 */
public class CommandAdmin implements CommandExecutor, TabCompleter {
    private final DiscordBMB plugin;

    public CommandAdmin(DiscordBMB plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Messages.getMessage("usage-admin-command"));
            return true;
        }

        if (!sender.hasPermission("discordbotmanager.reload")) {
            sender.sendMessage(Messages.getMessage("no-permission"));
            return true;
        }

        plugin.getConfigManager().reloadConfigs();

        plugin.getNettyService().closeNettyConnection();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String host = Settings.getVelocityHost();
            int port   = Settings.getVelocityPort();
            plugin.getNettyService().initializeNettyClient();
        });

        sender.sendMessage(Messages.getMessage("reload-success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
