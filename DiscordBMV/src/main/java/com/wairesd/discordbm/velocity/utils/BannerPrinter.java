package com.wairesd.discordbm.velocity.utils;

public class BannerPrinter {

    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_RED    = "\u001B[31m";
    private static final String ANSI_WHITE  = "\u001B[37m";

    public static void printBanner() {
        System.out.println(ANSI_PURPLE + " ____    " + ANSI_RED + " __  __ " + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "| __ )  " + ANSI_RED + "|  \\/  |" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "|  _ \\  " + ANSI_RED + "| |\\/| |" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "| |_) | " + ANSI_RED + "| |  | |" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "|____/  " + ANSI_RED + "|_|  |_|" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_WHITE + "    DiscordBMV v1.0" + ANSI_RESET);
        System.out.println(ANSI_WHITE + "    Running on Velocity" + ANSI_RESET);
        System.out.println();
    }
}
