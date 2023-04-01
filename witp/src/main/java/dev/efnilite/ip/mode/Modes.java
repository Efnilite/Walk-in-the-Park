package dev.efnilite.ip.mode;

import dev.efnilite.ip.IP;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultMode) IP.getRegistry().getMode("default");
        SPECTATOR = (SpectatorMode) IP.getRegistry().getMode("spectator");
    }
}