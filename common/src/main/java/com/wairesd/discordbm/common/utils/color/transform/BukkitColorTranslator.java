package com.wairesd.discordbm.common.utils.color.transform;

public final class BukkitColorTranslator {

    private static final boolean PRESENT = isBukkitPresentInternal();

    private BukkitColorTranslator() {}

    public static boolean isBukkitPresent() {
        return PRESENT;
    }

    public static String translate(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    private static boolean isBukkitPresentInternal() {
        try {
            Class.forName("org.bukkit.ChatColor");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
