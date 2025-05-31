package com.wairesd.discordbm.api.handle;

public interface DiscordCommandHandler {
    void handleCommand(String command, String[] args, String requestId);
}