package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player falls. Read-only.
 */
public class ParkourFallEvent extends EventWrapper {

    public final ParkourPlayer2 player;

    public ParkourFallEvent(ParkourPlayer2 player) {
        this.player = player;
    }
}