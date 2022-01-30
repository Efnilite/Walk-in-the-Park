package dev.efnilite.witp.events;

import dev.efnilite.fycore.event.EventWrapper;
import dev.efnilite.witp.player.ParkourPlayer;

/**
 * When a player falls
 */
public class PlayerFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}