package com.wairesd.discordbm.velocity.commandbuilder.command;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.config.configurators.Commands;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommandLoader {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public List<CommandStructured> load() {
        try {
            List<CommandStructured> commands = Commands.getCustomCommands();
            if (Settings.isDebugCommandRegistrations()) {
                logger.info("Loaded commands:");
                commands.forEach(cmd -> logger.info(" - {}", cmd.getName()));
            }
            return commands;
        } catch (Exception e) {
            logger.error("Failed to load commands", e);
            return List.of();
        }
    }
}
