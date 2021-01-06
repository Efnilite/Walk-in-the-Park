package dev.efnilite.witp.events;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.wrapper.EventWrapper;

/**
 * When a player scores
 */
public class PlayerScoreEvent extends EventWrapper {

    public ParkourPlayer player;

    public PlayerScoreEvent(ParkourPlayer player) {
        this.player = player;
    }
}
