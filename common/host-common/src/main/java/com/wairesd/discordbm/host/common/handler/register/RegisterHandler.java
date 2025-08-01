package com.wairesd.discordbm.host.common.handler.register;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wairesd.discordbm.common.models.register.RegisterMessage;
import com.wairesd.discordbm.host.common.models.command.CommandRegistrationService;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.database.Database;
import com.wairesd.discordbm.host.common.models.command.CommandDefinition;
import com.wairesd.discordbm.host.common.network.NettyServer;
import com.wairesd.discordbm.host.common.network.NettyServerHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RegisterHandler {
    private static final Logger logger = LoggerFactory.getLogger("DiscordBM");
    private final NettyServerHandler handler;
    private final Database dbManager;
    private final NettyServer nettyServer;
    private final Gson gson = new Gson();
    private boolean authenticated = false;
    private final CommandRegistrationService commandRegisterService;

    public RegisterHandler(NettyServerHandler handler, Database dbManager, NettyServer nettyServer) {
        this.handler = handler;
        this.dbManager = dbManager;
        this.nettyServer = nettyServer;
        this.commandRegisterService = nettyServer.getCommandRegistrationService();
    }

    public void handleRegister(ChannelHandlerContext ctx, String message, String ip, int port) {
        RegisterMessage<CommandDefinition> registerMessage = gson.fromJson(
            message,
            new TypeToken<RegisterMessage<CommandDefinition>>(){}.getType()
        );

        if (registerMessage.secret() == null || !registerMessage.secret().equals(Settings.getSecretCode())) {
            ctx.writeAndFlush("Error: Invalid secret code");
            dbManager.incrementFailedAttempt(ip);
            ctx.close();
            return;
        }

        String serverName = registerMessage.serverName();
        String pluginName = registerMessage.pluginName();
        List<CommandDefinition> commands = registerMessage.commands();

        nettyServer.setServerName(ctx.channel(), serverName);

        if (commands != null && !commands.isEmpty()) {
            for (CommandDefinition cmd : commands) {
                commandRegisterService.registerCommands(serverName, List.of(cmd), ctx.channel());
                
                String realPluginName = null;
                try {
                    var method = cmd.getClass().getMethod("pluginName");
                    Object value = method.invoke(cmd);
                    if (value instanceof String s && !s.isEmpty()) {
                        realPluginName = s;
                    }
                } catch (Exception ignored) {}
                if (realPluginName == null || realPluginName.isEmpty()) {
                    realPluginName = pluginName;
                }
                if (realPluginName != null && !realPluginName.isEmpty()) {
                    nettyServer.registerAddonCommand(cmd.name(), realPluginName);
                }
                
                if (Settings.isDebugCommandRegistrations()) {
                    logger.info("Registered command '{}' from plugin '{}' for server '{}'",
                            cmd.name(), realPluginName, serverName);
                }
            }
        }
    }
}
