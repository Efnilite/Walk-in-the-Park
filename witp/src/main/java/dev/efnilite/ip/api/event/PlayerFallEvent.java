package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player falls. Read-only.
 */
public class PlayerFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}