package com.wairesd.discordbm.host.common.commandbuilder.commands.processor;

import com.wairesd.discordbm.host.common.commandbuilder.core.models.structures.CommandStructured;
import com.wairesd.discordbm.host.common.models.command.CommandDefinition;
import com.wairesd.discordbm.host.common.models.option.OptionDefinition;
import com.wairesd.discordbm.host.common.network.NettyServer;

import java.util.List;
import java.util.Map;

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
                        .toList(),
                cmd.getPermission(),
                cmd.getConditions() != null ? cmd.getConditions().stream().map(c -> c instanceof Map ? (Map<String, Object>)c : null).toList() : List.of(),
                cmd.getPluginName()
        );
        nettyServer.getCommandDefinitions().put(cmd.getName(), def);
    }
}