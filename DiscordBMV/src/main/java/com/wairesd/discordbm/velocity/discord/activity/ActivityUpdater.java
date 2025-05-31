package com.wairesd.discordbm.velocity.discord.activity;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.LoggerFactory;

public class ActivityUpdater {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private final JDA jda;
    private final ActivityFactory activityFactory;

    public ActivityUpdater(JDA jda, ActivityFactory activityFactory) {
        this.jda = jda;
        this.activityFactory = activityFactory;
    }

    public void updateActivity(String activityType, String activityMessage) {
        if (jda != null) {
            Activity activity = activityFactory.createActivity(activityType, activityMessage);
            jda.getPresence().setActivity(activity);
            logger.info("Bot activity updated to: {} {}", activityType, activityMessage);
        } else {
            logger.warn("Cannot update activity — JDA not initialized");
        }
    }
}