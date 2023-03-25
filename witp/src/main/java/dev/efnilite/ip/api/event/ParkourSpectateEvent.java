package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player starts spectating a session. Read-only.
 */
public class ParkourSpectateEvent extends EventWrapper {

    public final ParkourPlayer player;

    public ParkourSpectateEvent(ParkourPlayer player) {
        this.player = player;
    }
}
