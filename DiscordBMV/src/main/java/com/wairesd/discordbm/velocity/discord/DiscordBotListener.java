package com.wairesd.discordbm.velocity.discord;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.discord.handle.CommandHandler;
import com.wairesd.discordbm.velocity.discord.request.RequestSender;
import com.wairesd.discordbm.velocity.discord.response.ResponseHelper;
import com.wairesd.discordbm.velocity.discord.selection.ServerSelector;
import com.wairesd.discordbm.velocity.network.NettyServer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class DiscordBotListener extends ListenerAdapter {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private final DiscordBMV plugin;
    private final NettyServer nettyServer;

    private final CommandHandler commandHandler;
    private final ServerSelector serverSelector;
    private final RequestSender requestSender;
    private final ResponseHelper responseHelper;

    public DiscordBotListener(DiscordBMV plugin, NettyServer nettyServer, PluginLogger logger) {
        this.plugin = plugin;
        this.nettyServer = nettyServer;

        this.requestSender = new RequestSender(nettyServer, logger);
        this.responseHelper = new ResponseHelper();
        this.commandHandler = new CommandHandler(plugin, requestSender, responseHelper);
        this.serverSelector = new ServerSelector(requestSender, responseHelper);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        logger.info("Received slash command: {}", command);

        logger.info("Checking servers for command '{}': {}", command, nettyServer.getServersForCommand(command).stream().map(NettyServer.ServerInfo::serverName).collect(Collectors.toList()));

        var servers = nettyServer.getServersForCommand(command);

        if (servers.isEmpty()) {
            commandHandler.handleCustomCommand(event, command);
            return;
        }

        var cmdDef = nettyServer.getCommandDefinitions().get(command);
        if (commandHandler.isCommandRestrictedToDM(event, cmdDef)) {
            responseHelper.replyCommandRestrictedToDM(event);
            return;
        }

        if (servers.size() == 1) {
            requestSender.sendRequestToServer(event, servers.get(0));
        } else {
            serverSelector.sendServerSelectionMenu(event, servers);
        }
    }

    public RequestSender getRequestSender() {
        return requestSender;
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!serverSelector.isValidSelectMenu(event)) return;

        serverSelector.handleSelection(event);
    }
}
