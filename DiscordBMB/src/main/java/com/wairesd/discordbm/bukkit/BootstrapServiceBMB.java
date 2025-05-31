package com.wairesd.discordbm.bukkit;

import com.wairesd.discordbm.api.DiscordBMAPI;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.bukkit.commands.CommandAdmin;
import com.wairesd.discordbm.bukkit.config.ConfigManager;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.utils.BannerPrinter;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import org.bukkit.Bukkit;

public class BootstrapServiceBMB {
    private final DiscordBMB plugin;
    private final PluginLogger logger;

    public BootstrapServiceBMB(DiscordBMB plugin, PluginLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void initialize() {
        BannerPrinter.printBanner();

        initConfig();
        initPlatform();
        initApi();
        registerCommands();
        initNettyAsync();
    }

    private void initConfig() {
        ConfigManager configManager = new ConfigManager(plugin);
        configManager.loadConfigs();
        plugin.setConfigManager(configManager);
        plugin.setServerName(Settings.getServerName());
        logger.info("Configuration initialized");
    }

    private void initPlatform() {
        Platform platform = new BukkitPlatform(plugin);
        plugin.setPlatform(platform);
        logger.info("Platform initialized");
    }

    private void initApi() {
        DiscordBMAPI api = new DiscordBMAPI(plugin.getPlatform(), logger);
        DiscordBMB.setApi(api);
        logger.info("DiscordBM API initialized");
    }

    private void registerCommands() {
        plugin.getCommand("discordBMB").setExecutor(new CommandAdmin(plugin));
        plugin.getCommand("discordBMB").setTabCompleter(new CommandAdmin(plugin));
        logger.info("Commands registered");
    }

    private void initNettyAsync() {
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getPlatform().getNettyService().initializeNettyClient();
            plugin.setNettyService(plugin.getPlatform().getNettyService());
            logger.info("Netty client initialized asynchronously");
        });
    }
}
