package com.wairesd.discordbm.bukkit;

import com.google.gson.Gson;
import com.wairesd.discordbm.api.*;
import com.wairesd.discordbm.api.handler.DiscordCommandHandler;
import com.wairesd.discordbm.api.listener.DiscordCRLB;
import com.wairesd.discordbm.api.models.command.Command;
import com.wairesd.discordbm.api.network.NettyService;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.bukkit.config.ConfigManager;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.placeholders.PlaceholderService;
import com.wairesd.discordbm.common.utils.logging.JavaPluginLogger;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DiscordBMB extends JavaPlugin {
    private final PluginLogger pluginLogger = new JavaPluginLogger(getLogger());
    private static DiscordBMAPI api;
    private ConfigManager configManager;
    private Platform platform;
    private final Map<String, DiscordCommandHandler> commandHandlers = new HashMap<>();
    private final List<Command> addonCommands = new ArrayList<>();
    private String serverName;
    private final Gson gson = new Gson();
    private boolean invalidSecret = false;
    private NettyService nettyService;
    private PlaceholderService placeholderService;

    private BootstrapDBMB bootstrapService;

    @Override
    public void onEnable() {
        bootstrapService = new BootstrapDBMB(this, pluginLogger);
        bootstrapService.initialize();
    }

    @Override
    public void onDisable() {
        if (platform != null && platform.getNettyService() != null) {
            platform.getNettyService().closeNettyConnection();
        }
    }

    public void registerCommandHandler(String command, DiscordCommandHandler handler, DiscordCRLB listener, Command addonCommand) {
        commandHandlers.put(command, handler);
        if (addonCommand != null) {
            synchronized (addonCommands) {
                addonCommands.add(addonCommand);
                if (Settings.isDebugCommandRegistrations()) {
                    getLogger().info("Registered addon command: " + addonCommand.getName());
                }
            }
        }
        if (listener != null && platform.getNettyService().getNettyClient() != null && platform.getNettyService().getNettyClient().isActive()) {
            listener.onNettyConnected();
        }
    }

    public void addAddonCommand(Command command) {
        synchronized (addonCommands) {
            addonCommands.add(command);
            if (Settings.isDebugCommandRegistrations()) {
                getLogger().info("Registered addon command: " + command.getName());
            }
        }
    }

    public List<Command> getAddonCommands() {
        synchronized (addonCommands) {
            return new ArrayList<>(addonCommands);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Platform getPlatform() {
        return platform;
    }

    public static DiscordBMAPI getApi() {
        return api;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public static void setApi(DiscordBMAPI apiInstance) {
        api = apiInstance;
    }

    public Map<String, DiscordCommandHandler> getCommandHandlers() {
        return Collections.unmodifiableMap(commandHandlers);
    }

    public NettyService getNettyService() {
        return nettyService;
    }

    public void setNettyService(NettyService nettyService) {
        this.nettyService = nettyService;
    }
}
