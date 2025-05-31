package com.wairesd.discordbm.common.models.unregister;

/**
 * Represents a message used to unregister a command within the server system.
 * This message contains the necessary details to identify the command and
 * authenticate the request.
 */
public class UnregisterMessage {
    public String type = "unregister";
    public String serverName;
    public String pluginName;
    public String commandName;
    public String secret;

    public UnregisterMessage(String serverName, String pluginName, String commandName, String secret) {
        this.serverName = serverName;
        this.pluginName = pluginName;
        this.commandName = commandName;
        this.secret = secret;
    }
}