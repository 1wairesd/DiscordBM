package com.wairesd.discordbm.velocity.commandbuilder.actions.buttons;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.FormButtonData;
import com.wairesd.discordbm.velocity.commandbuilder.registry.buttons.ButtonRegistry;
import com.wairesd.discordbm.velocity.commandbuilder.registry.buttons.FormButtonRegistry;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ButtonActionRegistry {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ButtonRegistry buttonRegistry = new ButtonRegistry();
    private final FormButtonRegistry formButtonRegistry = new FormButtonRegistry();

    public ButtonActionRegistry() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    private void cleanupExpiredEntries() {
        buttonRegistry.cleanupExpiredEntries();
        formButtonRegistry.cleanupExpiredEntries();
    }

    public void registerButton(String id, String message, long durationMillis) {
        buttonRegistry.register(id, message, durationMillis);
        if (Settings.isDebugButtonRegister()) {
            logger.info("Registered button: id={}", id);
        }
    }

    public void registerFormButton(String id, String formName, String messageTemplate, String requiredRoleId, long durationMillis) {
        formButtonRegistry.register(id, formName, messageTemplate, requiredRoleId, durationMillis);
        if (Settings.isDebugButtonRegister()) {
            logger.info("Registered form button: id={}", id);
        }
    }

    public String getMessage(String id) {
        return buttonRegistry.getMessage(id);
    }

    public FormButtonData getFormButtonData(String id) {
        return formButtonRegistry.getFormButtonData(id);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
