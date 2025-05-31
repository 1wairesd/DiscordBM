package com.wairesd.discordbm.velocity.commandbuilder.parser;

import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandParserFailAction {

    public static List<CommandAction> parse(Map<String, Object> cmdData, DiscordBMV plugin) {
        Object raw = cmdData.get("fail-actions");
        if (!(raw instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<String, Object>) item)
                .map(actionMap -> CommandParserAction.parseAction(actionMap, plugin))
                .collect(Collectors.toList());
    }
}
