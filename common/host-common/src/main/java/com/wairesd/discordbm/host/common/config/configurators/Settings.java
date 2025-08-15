package com.wairesd.discordbm.host.common.config.configurators;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.config.converter.ConfigConverter;
import com.wairesd.discordbm.host.common.config.migrator.SettingsMigrator;
import com.wairesd.discordbm.host.common.utils.SecretManager;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class Settings {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));
    private static final String CONFIG_FILE_NAME = "settings.yml";
    public static final String ROOT = "DiscordBM";
    private static final String DEFAULT_FORWARDING_SECRET_FILE = "secret.complete.code";

    private static File configFile;
    public static Map<String, Object> config;
    private static SecretManager secretManager;

    public static void init(File dataDir) {
        configFile = new File(dataDir, CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            try {
                Files.createDirectories(dataDir.toPath());
                try (var in = Settings.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                    if (in == null) {
                        throw new IllegalStateException(CONFIG_FILE_NAME + " not found in resources");
                    }
                    Files.copy(in, configFile.toPath());
                    logger.info("Default settings.yml copied from resources");
                }
            } catch (Exception e) {
                logger.error("Failed to create default settings.yml: {}", e.getMessage(), e);
            }
        }
        loadConfig();
        secretManager = new SecretManager(dataDir.toPath(), getForwardingSecretFile());
    }

    private static void loadConfig() {
        try {
            Yaml yaml = ConfigConverter.createFormattedYaml();
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                config = yaml.load(inputStream);
            }
            if (config == null) {
                config = new LinkedHashMap<>();
            }

            if (SettingsMigrator.migrateConfigIfNeeded()) {
                try (BufferedWriter writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
                    yaml.dump(config, writer);
                }
                logger.info("settings.yml migrated to latest version");
            }

            validateConfig();
        } catch (Exception e) {
            logger.error("Error loading settings.yml: {}", e.getMessage(), e);
        }
    }

    public static void reload() {
        loadConfig();
        secretManager = new SecretManager(configFile.getParentFile().toPath(), getForwardingSecretFile());
        logger.info("settings.yml reloaded successfully");
    }

    private static void validateConfig() {
        if (getBotToken().isEmpty()) {
            logger.warn("Bot-token missing in settings.yml, using default behavior");
        }
    }

    private static boolean getDebugOption(String path, boolean defaultValue) {
        return (boolean) getConfigValue(ROOT + ".debug." + path, defaultValue);
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

    public static boolean isDebugCommandReceived() {
        return getDebugOption("debug-command-received", false);
    }

    public static boolean isDebugCommandExecution() {
        return getDebugOption("debug-command-execution", false);
    }

    public static boolean isDebugResolvedMessages() {
        return getDebugOption("debug-resolved-messages", false);
    }

    public static boolean isDebugRequestProcessing() {
        return getDebugOption("debug-request-processing", false);
    }

    public static boolean isDebugCommandNotFound() {
        return getDebugOption("debug-command-not-found", false);
    }

    public static boolean isDebugNettyStart() {
        return getDebugOption("debug-netty-start", false);
    }

    public static boolean isDebugSendMessageAction() {
        return getDebugOption("debug-sendmessage-action", false);
    }

    public static boolean isDebugSendMessageToChannel() {
        return getDebugOption("debug-sendmessage-to-channel", false);
    }

    public static boolean isDebugAuthentication() {
        return getDebugOption("debug-authentication", true);
    }

    public static boolean isDebugErrors() {
        return getDebugOption("debug-errors", true);
    }

    public static boolean isDebugButtonRegister() {
        return getDebugOption("debug-button-register", false);
    }

    public static String getBotToken() {
        Object v = getConfigValue(ROOT + ".Discord.token", "");
        return String.valueOf(v);
    }

    public static int getNettyPort() {
        return SettingsMigrator.getIntConfigValue(ROOT + ".netty.port", 8080);
    }

    public static String getNettyIp() {
        Object v = getConfigValue(ROOT + ".netty.ip", "");
        return String.valueOf(v);
    }

    public static String getForwardingSecretFile() {
        Object v = getConfigValue(ROOT + ".forwarding-secret-file", DEFAULT_FORWARDING_SECRET_FILE);
        return String.valueOf(v);
    }

    public static String getSecretCode() {
        return secretManager != null ? secretManager.getSecretCode() : null;
    }

    public static String getActivityType() {
        return (String) getConfigValue(ROOT + ".Discord.activity.type", "playing");
    }

    public static String getActivityMessage() {
        return (String) getConfigValue(ROOT + ".Discord.activity.message", "Velocity Server");
    }

    public static boolean isMySQLEnabled() {
        return (boolean) getConfigValue(ROOT + ".mysql.enabled", false);
    }

    public static String getMySQLHost() {
        return (String) getConfigValue(ROOT + ".mysql.host", "localhost");
    }

    public static int getMySQLPort() {
        return (int) getConfigValue(ROOT + ".mysql.port", 3306);
    }

    public static String getMySQLDatabase() {
        return (String) getConfigValue(ROOT + ".mysql.database", "DiscordBM");
    }

    public static String getMySQLUsername() {
        return (String) getConfigValue(ROOT + ".mysql.username", "root");
    }

    public static String getMySQLPassword() {
        return (String) getConfigValue(ROOT + ".mysql.password", "password");
    }

    public static String getMySQLParams() {
        return (String) getConfigValue(ROOT + ".mysql.params", "?useSSL=false&serverTimezone=UTC");
    }

    public static String getDatabaseJdbcUrl(String sqlitePath) {
        if (isMySQLEnabled()) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s%s",
                    getMySQLHost(),
                    getMySQLPort(),
                    getMySQLDatabase(),
                    getMySQLParams()
            ) + String.format("&user=%s&password=%s", getMySQLUsername(), getMySQLPassword());
        } else {
            return "jdbc:sqlite:" + sqlitePath;
        }
    }

    public static Object getConfigValue(String path, Object defaultValue) {
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