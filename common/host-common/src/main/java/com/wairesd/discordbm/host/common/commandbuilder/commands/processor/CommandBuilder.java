package com.wairesd.discordbm.host.common.commandbuilder.commands.processor;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.structures.CommandStructured;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.LoggerFactory;

import static net.dv8tion.jda.api.interactions.commands.build.Commands.slash;

public class CommandBuilder {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));

    public SlashCommandData build(CommandStructured cmd) {
        SlashCommandData cmdData = slash(cmd.getName(), cmd.getDescription());
        addOptions(cmd, cmdData);
        setContext(cmd, cmdData);
        return cmdData;
    }

    private void addOptions(CommandStructured cmd, SlashCommandData data) {
        cmd.getOptions().forEach(opt -> {
            try {
                OptionType type = OptionType.valueOf(opt.getType().toUpperCase());
                data.addOption(type, opt.getName(), opt.getDescription(), opt.isRequired());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid option type '{}' in '{}'", opt.getType(), cmd.getName());
            }
        });
    }

    private void setContext(CommandStructured cmd, SlashCommandData data) {
        boolean guildOnly = switch (cmd.getContext()) {
            case "server" -> true;
            case "dm", "both" -> false;
            default -> {
                logger.warn("Unknown context '{}'", cmd.getContext());
                yield false;
            }
        };
        data.setGuildOnly(guildOnly);
    }
}