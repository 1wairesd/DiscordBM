package com.wairesd.discordbm.velocity.commandbuilder.parser;

import com.wairesd.discordbm.velocity.commandbuilder.conditions.chance.ChanceCondition;
import com.wairesd.discordbm.velocity.commandbuilder.conditions.permissions.HaveRoleCondition;
import com.wairesd.discordbm.velocity.commandbuilder.conditions.permissions.NotHaveRoleCondition;
import com.wairesd.discordbm.velocity.commandbuilder.models.codinations.CommandCondition;

import java.util.Map;

public class CommandParserCondition {
    public static CommandCondition parseCondition(Map<String, Object> conditionMap) {
        String type = (String) conditionMap.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Condition type is required");
        }
        return switch (type.toLowerCase()) {
            case "permission" -> new HaveRoleCondition(conditionMap);
            case "not_have_role" -> new NotHaveRoleCondition(conditionMap);
            case "chance" -> new ChanceCondition(conditionMap);
            default -> throw new IllegalArgumentException("Unknown condition type: " + type);
        };
    }
}
