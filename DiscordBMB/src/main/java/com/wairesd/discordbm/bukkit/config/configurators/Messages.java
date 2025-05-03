package com.wairesd.discordbm.bukkit.config.configurators;

import com.wairesd.discordbm.common.utils.ColorUtils;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The Messages class handles the loading, saving, and retrieval of text messages
 * from a YAML configuration file. This is intended for managing configurable message
 * strings that can be used in a Bukkit plugin.
 *
 * This class ensures that the "messages.yml" configuration file is created, loaded,
 * and accessed properly. Messages are color-coded using the ColorUtils class when retrieved.
 */
public class Messages {
    private static final Logger logger = LoggerFactory.getLogger(Messages.class);
    private static CommentedConfigurationNode messagesConfig;
    private static YamlConfigurationLoader loader;

    /**
     * Loads the "messages.yml" file from the plugin's data folder. If the file does not
     * exist, it attempts to create it by copying the default resource from the plugin's
     * jar. The method also loads the configuration data into memory for further use.
     *
     * @param plugin The JavaPlugin instance representing the plugin using this class.
     */
    public static void load(JavaPlugin plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    logger.warn("messages.yml not found in resources");
                }
            } catch (IOException e) {
                logger.error("Could not save messages.yml: {}", e.getMessage());
            }
        }

        loader = YamlConfigurationLoader.builder()
                .file(messagesFile)
                .build();

        try {
            messagesConfig = loader.load();
        } catch (ConfigurateException e) {
            logger.error("Failed to load messages.yml", e);
        }
    }

    public static void save() {
        try {
            loader.save(messagesConfig);
        } catch (ConfigurateException e) {
            logger.error("Failed to save messages.yml", e);
        }
    }

    public static String getMessage(String key) {
        String message = messagesConfig.node(key).getString("Message not found.");
        return ColorUtils.parseString(message);
    }
}
