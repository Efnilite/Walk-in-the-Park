package dev.efnilite.witp.events;

import dev.efnilite.fycore.event.EventWrapper;
import dev.efnilite.witp.player.ParkourUser;

/**
 * This event gets called when a player leaves the game.
 * This event is read-only.
 */
public class PlayerLeaveEvent extends EventWrapper {

    public final ParkourUser player;

    public PlayerLeaveEvent(ParkourUser player) {
        this.player = player;
    }
}
