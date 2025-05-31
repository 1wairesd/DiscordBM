package com.wairesd.discordbm.api.platform;

import com.wairesd.discordbm.api.listener.DiscordCommandRegistrationListener;
import com.wairesd.discordbm.api.handle.DiscordCommandHandler;
import com.wairesd.discordbm.api.models.command.Command;
import com.wairesd.discordbm.api.network.NettyService;

import java.util.List;
import java.util.Map;

public interface Platform {
    String getVelocityHost();
    int getVelocityPort();
    String getServerName();
    String getSecretCode();
    boolean isDebugCommandRegistrations();
    boolean isDebugClientResponses();
    boolean isDebugConnections();
    boolean isDebugErrors();
    NettyService getNettyService();
    void registerCommandHandler(String command, DiscordCommandHandler handler, DiscordCommandRegistrationListener listener, Command addonCommand);
    Map<String, DiscordCommandHandler> getCommandHandlers();
    boolean checkIfCanHandle(String playerName, List<String> placeholders);
    Map<String, String> getPlaceholderValues(String playerName, List<String> placeholders);
    void runTaskAsynchronously(Runnable task);
    void onNettyConnected();
}