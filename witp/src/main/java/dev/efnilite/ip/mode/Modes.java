package dev.efnilite.ip.mode;

import dev.efnilite.ip.api.Registry;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
    }
}