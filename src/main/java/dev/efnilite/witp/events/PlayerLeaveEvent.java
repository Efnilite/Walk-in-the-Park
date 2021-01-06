package dev.efnilite.witp.events;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.wrapper.EventWrapper;

/**
 * When a player scores
 */
public class PlayerLeaveEvent extends EventWrapper {

    public ParkourPlayer player;

    public PlayerLeaveEvent(ParkourPlayer player) {
        this.player = player;
    }
}
