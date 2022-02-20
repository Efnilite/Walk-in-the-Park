package dev.efnilite.witp.events;

import dev.efnilite.fycore.event.EventWrapper;
import dev.efnilite.witp.player.ParkourPlayer;

/**
 * This event gets called when a player falls off of the parkour.
 * This event is read-only.
 */
public class PlayerFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}