package com.wairesd.discordbm.bukkit.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.*;

public class PlaceholderService {

    private static final ConcurrentHashMap<String, CachedValue> placeholderCache = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public PlaceholderService(Plugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> {
                    long now = System.currentTimeMillis();
                    placeholderCache.entrySet().removeIf(entry -> now - entry.getValue().timestamp > 5000);
                },
                1200L, 1200L
        );
    }

    private static class CachedValue {
        String value;
        long timestamp;

        public CachedValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public boolean checkIfCanHandle(String playerName, List<String> placeholders) {
        OfflinePlayer player;
        if (isValidUUID(playerName)) {
            player = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
        } else {
            var onlinePlayer = Bukkit.getPlayerExact(playerName);
            if (onlinePlayer != null) {
                player = onlinePlayer;
            } else {
                return false;
            }
        }

        for (String placeholder : placeholders) {
            String key = player.getUniqueId() + ":" + placeholder;
            CachedValue cached = placeholderCache.get(key);
            String result;
            if (cached != null && System.currentTimeMillis() - cached.timestamp < 3000) {
                result = cached.value;
            } else {
                result = PlaceholderAPI.setPlaceholders(player, placeholder);
                placeholderCache.put(key, new CachedValue(result));
            }
            if (!result.equals(placeholder)) {
                return true;
            }
        }
        return false;
    }


    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Map<String, String> getPlaceholderValues(String playerName, List<String> placeholders) {
        OfflinePlayer player;
        if (isValidUUID(playerName)) {
            player = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
        } else {
            var onlinePlayer = Bukkit.getPlayerExact(playerName);
            if (onlinePlayer != null) {
                player = onlinePlayer;
            } else {
                return new HashMap<>();
            }
        }

        Future<Map<String, String>> future = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
            Map<String, String> values = new HashMap<>();
            for (String placeholder : placeholders) {
                String key = player.getUniqueId() + ":" + placeholder;
                CachedValue cached = placeholderCache.get(key);
                if (cached != null && System.currentTimeMillis() - cached.timestamp < 3000) {
                    values.put(placeholder, cached.value);
                } else {
                    String result = PlaceholderAPI.setPlaceholders(player, placeholder);
                    values.put(placeholder, result);
                    placeholderCache.put(key, new CachedValue(result));
                }
            }
            return values;
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

}
