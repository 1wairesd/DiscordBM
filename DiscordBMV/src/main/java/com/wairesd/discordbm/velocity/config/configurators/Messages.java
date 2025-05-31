package com.wairesd.discordbm.velocity.config.configurators;

import com.wairesd.discordbm.common.utils.color.ColorUtils;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Messages {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final String MESSAGES_FILE_NAME = "messages.yml";
    public static final String DEFAULT_MESSAGE = "Message not found.";

    private static Path dataDirectory;
    private static CommentedConfigurationNode messagesConfig;
    private static YamlConfigurationLoader loader;

    public static void init(Path dataDir) {
        dataDirectory = dataDir;
        loadMessages();
    }

    private static void loadMessages() {
        CompletableFuture.runAsync(() -> {
            try {
                Path messagesPath = dataDirectory.resolve(MESSAGES_FILE_NAME);
                if (!Files.exists(messagesPath)) {
                    createDefaultMessagesFile(messagesPath);
                }

                loader = YamlConfigurationLoader.builder()
                        .path(messagesPath)
                        .build();

                messagesConfig = loader.load();
            } catch (Exception e) {
                logger.error("Error loading {}: {}", MESSAGES_FILE_NAME, e.getMessage(), e);
            }
        });
    }

    private static void createDefaultMessagesFile(Path messagesPath) throws IOException {
        Files.createDirectories(dataDirectory);
        try (InputStream in = Messages.class.getClassLoader().getResourceAsStream(MESSAGES_FILE_NAME)) {
            if (in != null) {
                Files.copy(in, messagesPath);
            } else {
                logger.error("{} not found in resources!", MESSAGES_FILE_NAME);
            }
        }
    }

    public static void reload() {
        CompletableFuture.runAsync(() -> {
            logger.info("{} reloaded successfully", MESSAGES_FILE_NAME);
        });
    }


    public static @NotNull ComponentLike getParsedMessage(String key, String defaultValue) {
        String message = getMessage(key, defaultValue);
        return ColorUtils.parseComponent(message);
    }

    public static String getMessage(String key, String defaultValue) {
        if (messagesConfig == null) {
            return defaultValue != null ? defaultValue : DEFAULT_MESSAGE;
        }
        return messagesConfig.node(key).getString(defaultValue != null ? defaultValue : DEFAULT_MESSAGE);
    }
}
