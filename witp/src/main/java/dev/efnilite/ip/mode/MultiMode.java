package dev.efnilite.ip.mode;

import dev.efnilite.ip.session.Session;
import org.bukkit.entity.Player;

/**
 * Class for modes featuring multiple players.
 */
public interface MultiMode extends Mode {

    /**
     * Adds a player to this mode.
     *
     * @param player The player
     */
    void join(Player player, Session session);

    /**
     * Removes a player from this mode.
     *
     * @param player The player
     */
    void leave(Player player, Session session);
}
