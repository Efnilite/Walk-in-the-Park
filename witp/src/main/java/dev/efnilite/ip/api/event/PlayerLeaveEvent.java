package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player leaves a session. Read-only.
 */
public class PlayerLeaveEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerLeaveEvent(ParkourPlayer player) {
        this.player = player;
    }
}
