package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.internal.gamemode.DefaultGamemode;
import dev.efnilite.ip.internal.gamemode.SpectatorGamemode;

public class Gamemodes {

    public static DefaultGamemode DEFAULT;
    public static SpectatorGamemode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultGamemode) IP.getRegistry().getGamemode("default");
        SPECTATOR = (SpectatorGamemode) IP.getRegistry().getGamemode("spectator");
    }
}