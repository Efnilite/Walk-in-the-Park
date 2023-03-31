package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.gamemode.DefaultGamemode;
import dev.efnilite.ip.gamemode.SpectatorGamemode;

public class Modes {

    public static DefaultGamemode DEFAULT;
    public static SpectatorGamemode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultGamemode) IP.getRegistry().getMode("default");
        SPECTATOR = (SpectatorGamemode) IP.getRegistry().getMode("spectator");
    }
}