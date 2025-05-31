package com.wairesd.discordbm.bukkit.config.configurators;

import com.wairesd.discordbm.common.utils.logging.JavaPluginLogger;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

import static org.bukkit.Bukkit.getLogger;

/**
 * Manages the loading, saving, and accessing of configuration settings
 * stored in a settings.yml file. Provides utility methods to retrieve
 * application settings and debug options for a Bukkit plugin.
 */
public class Settings {
    private static final PluginLogger pluginLogger = new JavaPluginLogger(getLogger());
    private static CommentedConfigurationNode settingsConfig;
    private static ConfigurationLoader<CommentedConfigurationNode> loader;

    /**
     * Loads the "settings.yml" file from the plugin's data folder. If the file does not
     * exist, it creates the file by saving the default resource from the plugin's jar.
     * The method also initializes and loads the configuration data into memory.
     *
     * @param plugin The JavaPlugin instance representing the plugin using this class.
     */
    public static void load(JavaPlugin plugin) {
        File settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            plugin.saveResource("settings.yml", false);
        }

        loader = YamlConfigurationLoader.builder()
                .file(settingsFile)
                .build();

        try {
            settingsConfig = loader.load();
        } catch (ConfigurateException e) {
            pluginLogger.error("Failed to load settings.yml", e);
        }
    }

    public static void save() {
        try {
            loader.save(settingsConfig);
        } catch (ConfigurateException e) {
            pluginLogger.error("Failed to save settings.yml", e);
        }
    }

    public static String getVelocityHost() {
        return settingsConfig.node("velocity", "host").getString("127.0.0.1");
    }

    public static int getVelocityPort() {
        return settingsConfig.node("velocity", "port").getInt(8080);
    }

    public static String getServerName() {
        return settingsConfig.node("server").getString("ServerName");
    }

    public static String getSecretCode() {
        return settingsConfig.node("velocity", "secret").getString("");
    }

    // Debug settings from the query
    public static boolean isDebugConnections() {
        return settingsConfig.node("debug", "debug-connections").getBoolean(true);
    }

    public static boolean isDebugClientResponses() {
        return settingsConfig.node("debug", "debug-client-responses").getBoolean(false);
    }

    public static boolean isDebugCommandRegistrations() {
        return settingsConfig.node("debug", "debug-command-registrations").getBoolean(false);
    }

    public static boolean isDebugErrors() {
        return settingsConfig.node("debug", "debug-errors").getBoolean(true);
    }
}
