package dev.efnilite.witp.events;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.util.wrapper.EventWrapper;

/**
 * When a player falls
 */
public class PlayerFallEvent extends EventWrapper {

    public ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}