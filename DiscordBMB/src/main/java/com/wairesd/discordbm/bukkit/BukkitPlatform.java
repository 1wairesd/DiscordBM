package com.wairesd.discordbm.bukkit;

import com.wairesd.discordbm.api.handle.DiscordCommandHandler;
import com.wairesd.discordbm.api.listener.DiscordCommandRegistrationListener;
import com.wairesd.discordbm.api.models.command.Command;
import com.wairesd.discordbm.api.network.NettyService;
import com.wairesd.discordbm.api.platform.Platform;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.wairesd.discordbm.bukkit.placeholders.PlaceholderService;
import com.wairesd.discordbm.common.utils.logging.JavaPluginLogger;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import org.bukkit.Bukkit;

import java.util.*;

public class BukkitPlatform implements Platform {
    private final DiscordBMB plugin;
    private final NettyService nettyService;
    private final Map<String, DiscordCommandHandler> commandHandlers = new HashMap<>();
    private final PlaceholderService placeholderService;
    private final PluginLogger pluginLogger;
    private final Set<DiscordCommandRegistrationListener> listeners = new HashSet<>();

    public BukkitPlatform(DiscordBMB plugin) {
        this.plugin = plugin;
        this.pluginLogger = new JavaPluginLogger(Bukkit.getLogger());
        this.nettyService = new NettyService(this, pluginLogger);
        this.placeholderService = new PlaceholderService(plugin);
    }

    @Override
    public String getVelocityHost() {
        return Settings.getVelocityHost();
    }

    @Override
    public int getVelocityPort() {
        return Settings.getVelocityPort();
    }

    @Override
    public String getServerName() {
        return Settings.getServerName();
    }

    @Override
    public String getSecretCode() {
        return Settings.getSecretCode();
    }

    @Override
    public boolean isDebugCommandRegistrations() {
        return Settings.isDebugCommandRegistrations();
    }

    @Override
    public boolean isDebugClientResponses() {
        return Settings.isDebugClientResponses();
    }

    @Override
    public boolean isDebugConnections() {
        return Settings.isDebugConnections();
    }

    @Override
    public boolean isDebugErrors() {
        return Settings.isDebugErrors();
    }

    @Override
    public NettyService getNettyService() {
        return nettyService;
    }

    @Override
    public void registerCommandHandler(String command, DiscordCommandHandler handler, DiscordCommandRegistrationListener listener, Command addonCommand) {
        commandHandlers.put(command, handler);
        if (addonCommand != null) {
            plugin.addAddonCommand(addonCommand);
        }
        if (listener != null && listeners.add(listener)) {
            if (nettyService.getNettyClient() != null && nettyService.getNettyClient().isActive()) {
                listener.onNettyConnected();
            }
        }
    }

    @Override
    public void onNettyConnected() {
        for (DiscordCommandRegistrationListener listener : listeners) {
            listener.onNettyConnected();
        }
    }

    @Override
    public Map<String, DiscordCommandHandler> getCommandHandlers() {
        return Collections.unmodifiableMap(commandHandlers);
    }

    @Override
    public boolean checkIfCanHandle(String playerName, List<String> placeholders) {
        return placeholderService.checkIfCanHandle(playerName, placeholders);
    }

    @Override
    public Map<String, String> getPlaceholderValues(String playerName, List<String> placeholders) {
        return placeholderService.getPlaceholderValues(playerName, placeholders);
    }

    @Override
    public void runTaskAsynchronously(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}