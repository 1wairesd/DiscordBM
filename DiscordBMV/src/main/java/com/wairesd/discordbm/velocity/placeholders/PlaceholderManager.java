package com.wairesd.discordbm.velocity.placeholders;

import com.google.gson.Gson;
import com.wairesd.discordbm.velocity.config.configurators.Settings;
import com.wairesd.discordbm.velocity.database.DatabaseManager;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class PlaceholderManager {
    private final PlaceholderAPI placeholderAPI;
    private final DatabaseManager dbManager;
    private final Logger logger;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<UUID, Map<String, String>> placeholderCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> playerNameToId = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public PlaceholderManager(PlaceholderAPI placeholderAPI, DatabaseManager dbManager, Logger logger) {
        this.placeholderAPI = placeholderAPI;
        this.dbManager = dbManager;
        this.logger = logger;

        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            dbManager.executeSQL("""
                CREATE TABLE IF NOT EXISTS placeholder_cache (
                    uuid TEXT PRIMARY KEY,
                    username TEXT,
                    placeholders TEXT,
                    last_updated TIMESTAMP
                )""");
        } catch (SQLException e) {
            logger.error("Failed to initialize placeholder database", e);
        }
    }

    public CompletableFuture<String> resolvePlaceholders(String text, String playerName, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = resolvePlayerUUID(playerName);
                if (uuid == null) return text;

                String convertedText = text.replaceAll("\\{([^}]+)\\}", "%$1%");
                String result = placeholderAPI.formatPlaceholders(convertedText, uuid)
                        .get(Settings.getPlaceholderTimeout(), TimeUnit.SECONDS);
                return result.replaceAll("%([^%]+)%", "{$1}");
            } catch (Exception e) {
                logger.error("Error resolving placeholders: {}", e.getMessage());
                return text;
            }
        }, executor);
    }

    private UUID resolvePlayerUUID(String playerName) {
        return playerNameToId.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(playerName))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseGet(() -> {
                    UUID uuid = fetchUUIDFromDatabase(playerName);
                    if (uuid != null) {
                        playerNameToId.put(playerName.toLowerCase(), uuid);
                    }
                    return uuid;
                });
    }

    private UUID fetchUUIDFromDatabase(String playerName) {
        try {
            return dbManager.querySQL(
                    "SELECT uuid FROM players WHERE username = ?",
                    stmt -> {
                        stmt.setString(1, playerName);
                        ResultSet rs = stmt.executeQuery();
                        return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
                    },
                    playerName
            );
        } catch (SQLException e) {
            logger.error("Failed to fetch UUID from database for player {}", playerName, e);
            return null;
        }
    }
}