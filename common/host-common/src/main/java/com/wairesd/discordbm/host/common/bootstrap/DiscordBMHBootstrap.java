package com.wairesd.discordbm.host.common.bootstrap;

import com.wairesd.discordbm.api.DBMAPI;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.host.common.api.HostMessageSender;
import com.wairesd.discordbm.host.common.commandbuilder.commands.core.CommandManager;
import com.wairesd.discordbm.host.common.commandbuilder.components.buttons.listener.ButtonInteractionListener;
import com.wairesd.discordbm.host.common.commandbuilder.components.modal.listener.ModalInteractionListener;
import com.wairesd.discordbm.host.common.config.ConfigManager;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.database.Database;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.DiscordBotManager;
import com.wairesd.discordbm.host.common.discord.DiscordBMHPlatformManager;
import com.wairesd.discordbm.host.common.discord.response.ResponseHandler;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.ModalHandler;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.option.Modal;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.option.Reply;
import com.wairesd.discordbm.host.common.discord.response.handler.sender.option.Embed;
import com.wairesd.discordbm.host.common.network.NettyServer;
import com.wairesd.discordbm.host.common.scheduler.WebhookScheduler;
import com.wairesd.discordbm.host.common.api.HostDiscordBMAPIImpl;
import com.wairesd.discordbm.host.common.models.command.HostCommandRegistration;
import net.dv8tion.jda.api.JDA;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordBMHBootstrap {
    private final DiscordBMHPlatformManager platformManager;
    private final Path dataDirectory;
    private final PluginLogger logger;
    
    private NettyServer nettyServer;
    private Database dbManager;
    private CommandManager commandManager;
    private DiscordBotManager discordBotManager;

    public DiscordBMHBootstrap(DiscordBMHPlatformManager platformManager, Path dataDirectory,
                               PluginLogger logger) {
        this.platformManager = platformManager;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.discordBotManager = new DiscordBotManager();
        platformManager.setDiscordBotManager(discordBotManager);
    }

    public void initialize() {
        initConfig();
        initDatabase();
        initNetty();
        initDiscord();
        startNetty();
    }

    private void initConfig() {
        ConfigManager.init(dataDirectory);
        logger.info("Configuration initialized");
        WebhookScheduler.start();
    }

    private void initDatabase() {
        String sqlitePath = dataDirectory.resolve("DiscordBM.db").toString();
        String dbUrl = Settings.getDatabaseJdbcUrl(sqlitePath);
        dbManager = new Database(dbUrl);
        logger.info("Database initialized ({} mode)", Settings.isMySQLEnabled() ? "MySQL" : "SQLite");
        platformManager.attachDatabaseToManagers(dbManager);
    }

    private void initNetty() {
        nettyServer = new NettyServer(dbManager);
        platformManager.setNettyServer(nettyServer);
        logger.info("Netty server is initialized");
    }

    private void initDiscord() {
        String token = Settings.getBotToken();
        String activityType = Settings.getActivityType();
        String activityMessage = Settings.getActivityMessage();
        discordBotManager.initializeBot(token, activityType, activityMessage);

        JDA jda = discordBotManager.getJda();
        if (jda == null) {
            logger.error("Bot Discord initialization failed! Interrupt.");
            return;
        }

        logger.info("Discord bot initialized");
        jda.addEventListener(new ButtonInteractionListener(nettyServer, platformManager));
        Map<String, String> requestIdToCommand = new ConcurrentHashMap<>();
        DiscordBotListener listener = new DiscordBotListener(platformManager, nettyServer, logger, requestIdToCommand);
        Embed embed = new Embed(listener);
        ModalHandler modalHandler = new ModalHandler(listener);
        Modal modal = new Modal(listener, platformManager);
        Reply reply = new Reply(listener);
        jda.addEventListener(listener);
        jda.addEventListener(new ModalInteractionListener(platformManager, requestIdToCommand));

        nettyServer.setJda(jda);
        ResponseHandler.init(listener, platformManager);

        commandManager = new CommandManager(nettyServer, jda);
        platformManager.setCommandManager(commandManager);
        commandManager.loadAndRegisterCommands();

        HostCommandRegistration hostCommandRegistration = new HostCommandRegistration(discordBotManager);
        HostDiscordBMAPIImpl hostApi = new HostDiscordBMAPIImpl(hostCommandRegistration, null);
        HostMessageSender hostMessageSender = new HostMessageSender(jda, hostApi);
        DBMAPI.setInstance(new HostDiscordBMAPIImpl(hostCommandRegistration, hostMessageSender));
    }

    private void startNetty() {
        new Thread(nettyServer::start, "Netty-Server-Thread").start();
    }

    public NettyServer getNettyServer() {
        return nettyServer;
    }

    public Database getDatabase() {
        return dbManager;
    }
} 