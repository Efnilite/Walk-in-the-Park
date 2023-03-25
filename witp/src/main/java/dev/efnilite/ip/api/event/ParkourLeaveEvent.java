package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player leaves a session. Read-only.
 */
public class ParkourLeaveEvent extends EventWrapper {

    public final ParkourUser player;

    public ParkourLeaveEvent(ParkourUser player) {
        this.player = player;
    }
}
