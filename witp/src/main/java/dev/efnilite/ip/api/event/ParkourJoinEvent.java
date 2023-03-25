package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class ParkourJoinEvent extends EventWrapper {

    public final ParkourUser player;

    public ParkourJoinEvent(ParkourUser player) {
        this.player = player;
    }
}
