package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a point is scored. Read-only.
 */
public class ParkourScoreEvent extends EventWrapper {

    public final ParkourPlayer player;

    public ParkourScoreEvent(ParkourPlayer player) {
        this.player = player;
    }
}
