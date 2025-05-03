package com.wairesd.discordbm.bukkit.network;

import com.wairesd.discordbm.bukkit.DiscordBMB;
import com.wairesd.discordbm.bukkit.models.command.Command;
import com.wairesd.discordbm.common.models.embed.EmbedDefinition;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.bukkit.config.configurators.Settings;
import com.google.gson.Gson;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

public class NettyService {
    private final DiscordBMB plugin;
    private final Gson gson = new Gson();
    private NettyClient nettyClient;

    public NettyService(DiscordBMB plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes and connects a Netty client with the specified host and port.
     * If the connection attempt fails, the error will be logged based on the debug settings.
     *
     * @param host the hostname or IP address of the Netty server to connect to
     * @param port the port number of the Netty server
     */
    public void initializeNettyClient(String host, int port) {
        nettyClient = new NettyClient(new InetSocketAddress(host, port), plugin);
        try {
            nettyClient.connect();
        } catch (Exception e) {
            if (Settings.isDebugErrors()) {
                plugin.getLogger().warning("Failed to connect to Velocity Netty server: " + e.getMessage());
            }
        }
    }

    /**
     * Closes the active Netty client connection associated with this service, if one exists.
     *
     * This method checks whether the {@code nettyClient} instance is active before attempting
     * to close it. If the connection is closed successfully, the {@code nettyClient} field is
     * set to {@code null}. Additionally, if debug connection logging is enabled via the
     * {@code Settings.isDebugConnections()} method, a log message will be recorded to indicate
     * the successful closure of the connection.
     */
    public void closeNettyConnection() {
        if (nettyClient != null && nettyClient.isActive()) {
            nettyClient.close();
            nettyClient = null;
            if (Settings.isDebugConnections()) {
                plugin.getLogger().info("Netty connection closed.");
            }
        }
    }

    /**
     * Sends a response message containing an embedded JSON object to the connected Netty server if the client is active.
     * The embedded JSON is deserialized into an {@code EmbedDefinition} object and included in the response message.
     * If the Netty client connection is inactive, the response will not be sent.
     *
     * @param requestId  the unique identifier for the request that triggered this response
     * @param embedJson  the JSON string representing the embed object to include in the response
     */
    public void sendResponse(String requestId, String embedJson) {
        if (nettyClient != null && nettyClient.isActive()) {
            EmbedDefinition embedObj = gson.fromJson(embedJson, EmbedDefinition.class);
            ResponseMessage respMsg = new ResponseMessage("response", requestId, null, embedObj);
            nettyClient.send(gson.toJson(respMsg));
        }
    }

    /**
     * Sends a message to the connected Netty server if the client is active.
     * If the Netty connection is inactive, the message will not be sent, and a warning
     * will be logged if debug error logging is enabled in the settings.
     *
     * @param message the message to be sent to the Netty server
     */
    public void sendNettyMessage(String message) {
        if (nettyClient != null && nettyClient.isActive()) {
            nettyClient.send(message);
        } else {
            if (Settings.isDebugErrors()) {
                plugin.getLogger().warning("Netty connection not active. Message not sent: " + message);
            }
        }
    }

    /**
     * Sends all addon commands to a connected Netty server. This method constructs
     * a registration message containing the provided addon commands and sends it
     * if the Netty client is active and the list of commands is not empty.
     *
     * @param addonCommands a list of commands to be sent for registration. Each command
     *                      contains details such as name, description, plugin name, context,
     *                      and options.
     * @param serverName    the name of the server to which the addon commands are being sent.
     */
    public void sendAllAddonCommands(List<Command> addonCommands, String serverName) {
        if (nettyClient != null && nettyClient.isActive() && !addonCommands.isEmpty()) {
            String secret = Settings.getSecretCode();
            RegisterMessage<Command> msg = new RegisterMessage<>(
                    "register",
                    serverName,
                    plugin.getName(),
                    addonCommands,
                    secret,
                    Optional.empty()
            );
            nettyClient.send(gson.toJson(msg));
            if (Settings.isDebugCommandRegistrations()) {
                plugin.getLogger().info(
                        "Sent registration message for " + addonCommands.size() + " addon commands."
                );
            }
        }
    }

    /**
     * Retrieves the instance of the NettyClient associated with this service.
     *
     * @return the NettyClient instance used for managing the connection to the Velocity proxy
     */
    public NettyClient getNettyClient() {
        return nettyClient;
    }
}
