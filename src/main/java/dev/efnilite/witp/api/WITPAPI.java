package dev.efnilite.witp.api;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;

public class WITPAPI {

    private WITPAPI() throws IllegalAccessException {
        throw new IllegalAccessException("Initializing API class");
    }

    /**
     * Gets the registry, used in addons.
     *
     * @return the {@link Registry} class
     */
    public static Registry getRegistry() {
        return WITP.getRegistry();
    }

    /**
     * Registers a player and makes a player join the world
     *
     * @param   player
     *          The player
     *
     * @return The registered version of the player, even if the player is already registered
     * @throws  IOException
     *          If something goes wrong with reading the player's file
     */
    public static @NotNull ParkourPlayer registerPlayer(@NotNull Player player) throws IOException, SQLException {
        return ParkourPlayer.register(player, null);
    }

    /**
     * Unregisters a player and kicks the player from the world (or sends them back to the server assigned in config.yml)
     *
     * @param   player
     *          The Bukkit version of the player
     *
     * @param   sendBack
     *          If the player should be sent back, usually true when working with APIs (false is when the player leaves)
     *          
     * @throws IOException If saving the file of the player goes wrong
     */
    public static void unregisterPlayer(@NotNull Player player, boolean sendBack) throws IOException, InvalidStatementException {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) {
            Logging.error("Player " + player.getName() + " isn't registered!");
            return;
        }
        ParkourPlayer.unregister(pp, sendBack, false, true);
    }

    /**
     * Unregisters a player and kicks the player from the world (or sends them back to the server assigned in config.yml)
     *
     * @param   player
     *          The player
     *
     * @param   sendBack
     *          If the player should be sent back, usually true when working with APIs (false is when the player leaves)
     *
     * @throws IOException If saving the file of the player goes wrong
     */
    public static void unregisterPlayer(@NotNull ParkourPlayer player, boolean sendBack) throws IOException, InvalidStatementException {
        ParkourPlayer.unregister(player, sendBack, false, true);
    }

    /**
     * Gets a player
     * @see ParkourPlayer
     *
     * @param   player
     *          The Bukkit version of the player
     *
     * @return the {@link ParkourPlayer} version of the player
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        return ParkourPlayer.getPlayer(player);
    }
}
