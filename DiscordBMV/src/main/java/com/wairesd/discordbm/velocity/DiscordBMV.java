package com.wairesd.discordbm.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.CommandManager;
import com.wairesd.discordbm.velocity.commandbuilder.models.pages.Page;
import com.wairesd.discordbm.velocity.config.configurators.Pages;
import com.wairesd.discordbm.velocity.config.configurators.Commands;
import com.wairesd.discordbm.velocity.discord.DiscordBotManager;
import com.wairesd.discordbm.velocity.network.NettyServer;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "discordbmv", name = "DiscordBMV", version = "1.0", authors = {"wairesd"})
public class DiscordBMV {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private final Path dataDirectory;
    private final ProxyServer proxy;

    private BootstrapServiceBMV bootstrapService;

    private final Map<String, String> globalMessageLabels = new HashMap<>();
    private final Map<String, Object> formHandlers = new ConcurrentHashMap<>();

    public static DiscordBMV plugin;
    public static Map<String, Page> pageMap = Pages.pageMap;

    @Inject
    public DiscordBMV(@DataDirectory Path dataDirectory, ProxyServer proxy) {
        this.dataDirectory = dataDirectory;
        this.proxy = proxy;
        Commands.plugin = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        plugin = this;
        Commands.plugin = this;

        bootstrapService = new BootstrapServiceBMV(this, dataDirectory, proxy, logger);
        bootstrapService.initialize();
    }

    public void updateActivity() {
        bootstrapService.getDiscordBotManager().updateActivity(
                com.wairesd.discordbm.velocity.config.configurators.Settings.getActivityType(),
                com.wairesd.discordbm.velocity.config.configurators.Settings.getActivityMessage()
        );
    }

    public void setGlobalMessageLabel(String key, String channelId, String messageId) {
        globalMessageLabels.put(key, channelId + ":" + messageId);
    }

    public String[] getMessageReference(String key) {
        String value = globalMessageLabels.get(key);
        if (value == null) return null;
        return value.contains(":") ? value.split(":", 2) : new String[]{null, value};
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public BootstrapServiceBMV getBootstrapService() {
        return bootstrapService;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public String getGlobalMessageLabel(String key) {
        return globalMessageLabels.get(key);
    }

    public Map<String, Object> getFormHandlers() {
        return formHandlers;
    }

    public static DiscordBMV getPluginInstance() {
        return plugin;
    }

    public PluginLogger getLogger() {
        return logger;
    }

    public NettyServer getNettyServer() {
        return bootstrapService != null ? bootstrapService.getNettyServer() : null;
    }

    public DiscordBotManager getDiscordBotManager() {
        return bootstrapService.getDiscordBotManager();
    }

    public CommandManager getCommandManager() {
        return bootstrapService.getCommandManager();
    }
}