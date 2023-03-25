package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class ParkourJoinEvent extends EventWrapper {

    public final ParkourPlayer player;

    public ParkourJoinEvent(ParkourPlayer player) {
        this.player = player;
    }
}
