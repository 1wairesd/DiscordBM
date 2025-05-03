package com.wairesd.discordbm.velocity.config.configurators;

import com.wairesd.discordbm.velocity.utils.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class Settings {
    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final String CONFIG_FILE_NAME = "settings.yml";
    private static final String DEFAULT_FORWARDING_SECRET_FILE = "secret.complete.code";

    private static File configFile;
    private static Map<String, Object> config;
    private static SecretManager secretManager;

    public static void init(File dataDir) {
        configFile = new File(dataDir, CONFIG_FILE_NAME);
        loadConfig();
        secretManager = new SecretManager(dataDir.toPath(), getForwardingSecretFile());
    }

    private static void loadConfig() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            Yaml yaml = new Yaml();
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                config = yaml.load(inputStream);
            }

            validateConfig();
            logger.info("Settings loaded from {}", configFile.getPath());
        } catch (Exception e) {
            logger.error("Error loading settings.yml: {}", e.getMessage(), e);
        }
    }

    private static void createDefaultConfig() throws IOException {
        configFile.getParentFile().mkdirs();
        try (InputStream inputStream = Settings.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (inputStream != null) {
                Files.copy(inputStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Default config loaded from resources to {}", configFile.getPath());
            } else {
                logger.error("{} not found in resources!", CONFIG_FILE_NAME);
                throw new IOException(CONFIG_FILE_NAME + " not found in resources");
            }
        }
    }

    public static void reload() {
        loadConfig();
        secretManager = new SecretManager(configFile.getParentFile().toPath(), getForwardingSecretFile());
        Messages.reload();
    }

    private static void validateConfig() {
        if (config == null || !config.containsKey("Discord") || !((Map) config.get("Discord")).containsKey("Bot-token")) {
            logger.warn("Bot-token missing in settings.yml, using default behavior");
        }
    }

    // Debug options
    private static boolean getDebugOption(String path, boolean defaultValue) {
        return (boolean) getConfigValue("debug." + path, defaultValue);
    }

    public static boolean isDebugConnections() {
        return getDebugOption("debug-connections", true);
    }

    public static boolean isDebugClientResponses() {
        return getDebugOption("debug-client-responses", false);
    }

    public static boolean isDebugPluginConnections() {
        return getDebugOption("debug-plugin-connections", false);
    }

    public static boolean isDebugCommandRegistrations() {
        return getDebugOption("debug-command-registrations", false);
    }

    public static boolean isDebugAuthentication() {
        return getDebugOption("debug-authentication", true);
    }

    public static boolean isDebugErrors() {
        return getDebugOption("debug-errors", true);
    }

    // Configuration getters
    public static String getBotToken() {
        return (String) getConfigValue("Discord.Bot-token", "");
    }

    public static int getNettyPort() {
        return (int) getConfigValue("netty.port", 0);
    }

    public static String getForwardingSecretFile() {
        return (String) getConfigValue("forwarding-secret-file", DEFAULT_FORWARDING_SECRET_FILE);
    }

    public static String getSecretCode() {
        return secretManager != null ? secretManager.getSecretCode() : null;
    }

    public static String getActivityType() {
        return (String) getConfigValue("Discord.activity.type", "playing");
    }

    public static String getActivityMessage() {
        return (String) getConfigValue("Discord.activity.message", "Velocity Server");
    }

    public static boolean isViewConnectedBannedIp() {
        return (boolean) getConfigValue("view_connected_banned_ip", false);
    }

    public static boolean shouldBypassCache() {
        return (boolean) getConfigValue("cache.bypass", false);
    }

    public static int getPlaceholderTimeout() {
        return (int) getConfigValue("placeholder-timeout", 10);
    }

    // Utility method to get values from the config
    private static Object getConfigValue(String path, Object defaultValue) {
        String[] keys = path.split("\\.");
        Object current = config;

        for (int i = 0; i < keys.length; i++) {
            if (!(current instanceof Map)) {
                return defaultValue;
            }

            Map<?, ?> map = (Map<?, ?>) current;
            current = map.get(keys[i]);

            if (current == null) {
                return defaultValue;
            }

            if (i == keys.length - 1) {
                return current;
            }
        }

        return defaultValue;
    }

}
