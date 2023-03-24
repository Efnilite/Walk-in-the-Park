package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class PlayerJoinEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerJoinEvent(ParkourPlayer player) {
        this.player = player;
    }
}
