package com.wairesd.discordbm.velocity.discord.activity;

import net.dv8tion.jda.api.entities.Activity;

public class ActivityFactory {

    public Activity createActivity(String type, String message) {
        if (type == null) type = "";
        return switch (type.toLowerCase()) {
            case "playing" -> Activity.playing(message);
            case "watching" -> Activity.watching(message);
            case "listening" -> Activity.listening(message);
            case "competing" -> Activity.competing(message);
            default -> Activity.playing(message);
        };
    }
}