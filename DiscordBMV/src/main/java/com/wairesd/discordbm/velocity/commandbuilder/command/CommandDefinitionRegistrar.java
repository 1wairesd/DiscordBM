package com.wairesd.discordbm.velocity.commandbuilder.command;

import com.wairesd.discordbm.velocity.commandbuilder.models.structures.CommandStructured;
import com.wairesd.discordbm.velocity.models.command.CommandDefinition;
import com.wairesd.discordbm.velocity.models.option.OptionDefinition;
import com.wairesd.discordbm.velocity.network.NettyServer;

public class CommandDefinitionRegistrar {
    private final NettyServer nettyServer;

    public CommandDefinitionRegistrar(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    public void register(CommandStructured cmd) {
        CommandDefinition def = new CommandDefinition(
                cmd.getName(),
                cmd.getDescription(),
                cmd.getContext(),
                cmd.getOptions().stream()
                        .map(opt -> new OptionDefinition(opt.getName(), opt.getType(), opt.getDescription(), opt.isRequired()))
                        .toList()
        );
        nettyServer.getCommandDefinitions().put(cmd.getName(), def);
    }
}