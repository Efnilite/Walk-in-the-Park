package dev.efnilite.witp.events;

import dev.efnilite.fycore.event.EventWrapper;
import dev.efnilite.witp.player.ParkourPlayer;

/**
 * When a player scores
 */
public class PlayerScoreEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerScoreEvent(ParkourPlayer player) {
        this.player = player;
    }
}
