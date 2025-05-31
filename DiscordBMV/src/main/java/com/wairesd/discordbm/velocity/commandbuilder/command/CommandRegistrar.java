package com.wairesd.discordbm.velocity.commandbuilder.command;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.LoggerFactory;

public class CommandRegistrar {
    private final JDA jda;
    private final CommandBuilder builder;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public CommandRegistrar(JDA jda, CommandBuilder builder) {
        this.jda = jda;
        this.builder = builder;
    }

    public boolean register(CommandStructured cmd) {
        try {
            SlashCommandData data = builder.build(cmd);
            jda.upsertCommand(data).queue();
            return true;
        } catch (Exception e) {
            logger.error("Failed to register command '{}'", cmd.getName(), e);
            return false;
        }
    }
}
