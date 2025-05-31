package com.wairesd.discordbm.velocity.discord;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.discord.activity.ActivityFactory;
import com.wairesd.discordbm.velocity.discord.activity.ActivityUpdater;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class DiscordBotManager {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private JDA jda;
    private boolean initialized = false;

    public DiscordBotManager() {
    }

    public void initializeBot(String token, String activityType, String activityMessage) {
        if (token == null || token.isEmpty()) {
            logger.error("Bot token is not specified!");
            return;
        }
        if (initialized) {
            logger.warn("Bot is already initialized!");
            return;
        }

        try {
            ActivityFactory activityFactory = new ActivityFactory();
            Activity activity = activityFactory.createActivity(activityType, activityMessage);

            jda = JDABuilder.createDefault(token)
                    .enableIntents(EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_MEMBERS
                    ))
                    .setActivity(activity)
                    .build()
                    .awaitReady();

            logger.info("JDA initialized");
            initialized = true;
        } catch (Exception e) {
            logger.error("Error initializing JDA: {}", e.getMessage(), e);
            jda = null;
            initialized = false;
        }
    }

    public void updateActivity(String activityType, String activityMessage) {
        if (!initialized || jda == null) {
            logger.warn("JDA is not initialized — cannot update activity");
            return;
        }
        ActivityFactory activityFactory = new ActivityFactory();
        ActivityUpdater activityUpdater = new ActivityUpdater(jda, activityFactory);
        activityUpdater.updateActivity(activityType, activityMessage);
    }

    public JDA getJda() {
        if (!initialized || jda == null) {
            logger.warn("JDA is not initialized yet!");
            return null;
        }
        return jda;
    }
}
