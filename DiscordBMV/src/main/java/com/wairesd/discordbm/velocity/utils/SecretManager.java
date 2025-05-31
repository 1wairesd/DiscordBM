package com.wairesd.discordbm.velocity.utils;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public class SecretManager {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final SecureRandom random = new SecureRandom();

    private final Path secretFilePath;
    private final String secretCode;

    public SecretManager(Path dataDirectory, String secretFileName) {
        this.secretFilePath = dataDirectory.resolve(secretFileName);
        this.secretCode = loadOrGenerateSecretCode();
    }

    private String loadOrGenerateSecretCode() {
        try {
            if (Files.exists(secretFilePath)) {
                String loadedCode = Files.readString(secretFilePath).trim();
                return loadedCode;
            } else {
                String rawSecret = generateRawSecretCode();
                String base64 = Base64.getEncoder().encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8));
                Files.writeString(secretFilePath, base64);
                logger.info("Generated new Base64 secret code and saved to {}", secretFilePath.getFileName());
                return base64;
            }
        } catch (IOException e) {
            logger.error("Error handling secret code file {}: {}", secretFilePath.getFileName(), e.getMessage(), e);
            return null;
        }
    }

    private String generateRawSecretCode() {
        int length = 15 + random.nextInt(16);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int codePoint;
            do {
                codePoint = random.nextInt(Character.MAX_CODE_POINT + 1);
            } while (!Character.isDefined(codePoint) || Character.isISOControl(codePoint));

            sb.appendCodePoint(codePoint);
        }

        return sb.toString();
    }

    public String getSecretCode() {
        return secretCode;
    }
}
