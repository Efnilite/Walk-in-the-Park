package dev.efnilite.witp.events;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.wrapper.EventWrapper;

/**
 * When a player falls
 */
public class PlayerFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}