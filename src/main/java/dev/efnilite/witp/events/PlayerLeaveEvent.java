package dev.efnilite.witp.events;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.wrapper.EventWrapper;

/**
 * When a player scores
 */
public class PlayerLeaveEvent extends EventWrapper {

    public final ParkourUser player;

    public PlayerLeaveEvent(ParkourUser player) {
        this.player = player;
    }
}
