package com.wairesd.discordbm.velocity.commandbuilder.conditions.chance;

import com.wairesd.discordbm.velocity.commandbuilder.models.codinations.CommandCondition;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

import java.util.Map;
import java.util.Random;

public class ChanceCondition implements CommandCondition {
    private final int percent;
    private final Random random = new Random();

    public ChanceCondition(Map<String, Object> properties) {
        Object percentObj = properties.get("percent");
        if (percentObj == null) {
            throw new IllegalArgumentException("Percent is required for ChanceCondition");
        }
        String percentStr = percentObj.toString();
        try {
            this.percent = Integer.parseInt(percentStr);
            if (this.percent < 0 || this.percent > 100) {
                throw new IllegalArgumentException("Percent must be between 0 and 100");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid percent value: " + percentStr);
        }
    }

    @Override
    public boolean check(Context context) {
        return random.nextInt(100) < percent;
    }
}