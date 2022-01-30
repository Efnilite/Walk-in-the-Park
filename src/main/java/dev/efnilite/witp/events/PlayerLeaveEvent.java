package dev.efnilite.witp.events;

import dev.efnilite.fycore.event.EventWrapper;
import dev.efnilite.witp.player.ParkourUser;

/**
 * When a player scores
 */
public class PlayerLeaveEvent extends EventWrapper {

    public final ParkourUser player;

    public PlayerLeaveEvent(ParkourUser player) {
        this.player = player;
    }
}
