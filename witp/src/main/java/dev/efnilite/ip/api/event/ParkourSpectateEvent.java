package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player starts spectating a session. Read-only.
 */
public class ParkourSpectateEvent extends EventWrapper {

    public final ParkourSpectator player;

    public ParkourSpectateEvent(ParkourSpectator player) {
        this.player = player;
    }
}
