package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a point is scored. Read-only.
 */
public class ParkourScoreEvent extends EventWrapper {

    public final ParkourPlayer2 player;

    public ParkourScoreEvent(ParkourPlayer2 player) {
        this.player = player;
    }
}
