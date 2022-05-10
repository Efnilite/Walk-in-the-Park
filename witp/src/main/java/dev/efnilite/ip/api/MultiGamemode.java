package dev.efnilite.ip.api;

import dev.efnilite.ip.session.Session;
import org.bukkit.entity.Player;

/**
 * Class for Gamemodes featuring multiple players.
 */
public interface MultiGamemode extends Gamemode {

    /**
     * Adds a player to this gamemode.
     *
     * @param   player
     *          The player
     */
    void join(Player player, Session session);

    /**
     * Removes a player from this gamemode.
     *
     * @param   player
     *          The player
     */
    void leave(Player player, Session session);
}
