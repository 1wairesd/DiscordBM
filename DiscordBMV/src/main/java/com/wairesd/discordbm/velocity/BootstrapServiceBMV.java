package com.wairesd.discordbm.velocity;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.CommandManager;
import com.wairesd.discordbm.velocity.commandbuilder.listeners.buttons.ButtonInteractionListener;
import com.wairesd.discordbm.velocity.commandbuilder.listeners.modals.ModalInteractionListener;
import com.wairesd.discordbm.velocity.commands.CommandAdmin;
import com.wairesd.discordbm.velocity.config.ConfigManager;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.config.configurators.Commands;
import com.wairesd.discordbm.velocity.database.DatabaseManager;
import com.wairesd.discordbm.velocity.discord.DiscordBotListener;
import com.wairesd.discordbm.velocity.discord.DiscordBotManager;
import com.wairesd.discordbm.velocity.discord.response.ResponseHandler;
import com.wairesd.discordbm.velocity.network.NettyServer;
import com.wairesd.discordbm.velocity.utils.BannerPrinter;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.JDA;

import java.nio.file.Path;

public class BootstrapServiceBMV {
    private final DiscordBMV plugin;
    private final Path dataDirectory;
    private final ProxyServer proxy;
    private final PluginLogger logger;

    private NettyServer nettyServer;
    private DatabaseManager dbManager;
    private CommandManager commandManager;
    private DiscordBotManager discordBotManager;

    public BootstrapServiceBMV(DiscordBMV plugin, Path dataDirectory, ProxyServer proxy, PluginLogger logger) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
        this.proxy = proxy;
        this.logger = logger;
        this.discordBotManager = new DiscordBotManager();
    }

    public void initialize() {
        Commands.plugin = plugin;
        BannerPrinter.printBanner();

        initConfig();
        initDatabase();
        initNetty();
        initCommands();
        initDiscord();
        startNetty();
    }

    private void initConfig() {
        ConfigManager.init(dataDirectory);
        logger.info("Configuration initialized");
    }

    private void initDatabase() {
        String dbPath = "jdbc:sqlite:" + dataDirectory.resolve("DiscordBMV.db");
        dbManager = new DatabaseManager(dbPath);
        logger.info("Database initialized");
    }

    private void initNetty() {
        nettyServer = new NettyServer(dbManager);
        logger.info("Netty server initialized");
    }

    private void initCommands() {
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("discordBMV").build(),
                new CommandAdmin(plugin)
        );
    }

    private void initDiscord() {
        String token = Settings.getBotToken();
        String activityType = Settings.getActivityType();
        String activityMessage = Settings.getActivityMessage();
        discordBotManager.initializeBot(token, activityType, activityMessage);

        JDA jda = discordBotManager.getJda();
        if (jda == null) {
            logger.error("Failed to initialize Discord bot! Aborting.");
            return;
        }

        logger.info("Discord bot initialized");
        jda.addEventListener(new ButtonInteractionListener());
        jda.addEventListener(new ModalInteractionListener());

        nettyServer.setJda(jda);
        DiscordBotListener listener = new DiscordBotListener(plugin, nettyServer, logger);
        jda.addEventListener(listener);
        ResponseHandler.init(listener);

        commandManager = new CommandManager(nettyServer, jda);
        commandManager.loadAndRegisterCommands();
    }

    private void startNetty() {
        new Thread(nettyServer::start, "Netty-Server-Thread").start();
    }

    public DiscordBotManager getDiscordBotManager() {
        return discordBotManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public NettyServer getNettyServer() {
        return nettyServer;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }
}