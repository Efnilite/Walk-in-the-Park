package dev.efnilite.ip.events;

import dev.efnilite.vilib.event.EventWrapper;
import dev.efnilite.ip.player.ParkourPlayer;

/**
 * This event gets called when a scores a point.
 * This event is read-only.
 */
public class PlayerScoreEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerScoreEvent(ParkourPlayer player) {
        this.player = player;
    }
}
