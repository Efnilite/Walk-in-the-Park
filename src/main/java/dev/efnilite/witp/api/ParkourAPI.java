package dev.efnilite.witp.api;

import dev.efnilite.fycore.sql.InvalidStatementException;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.ParkourMenu;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Main API accessor.
 *
 * @see ParkourMenu
 * @see ParkourUser
 */
@SuppressWarnings("unused")
public class ParkourAPI {

    private ParkourAPI() throws IllegalAccessException {
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
     * Deprecated. Use {@link ParkourUser#register(Player)}.
     *
     * @param   player
     *          The player
     *
     * @return The registered version of the player, even if the player is already registered
     */
    @Deprecated
    public static @NotNull ParkourPlayer registerPlayer(@NotNull Player player) throws IOException, SQLException {
        return ParkourPlayer.register(player);
    }

    /**
     * Deprecated. Use {@link ParkourUser#unregister(ParkourUser, boolean, boolean, boolean)}.
     *
     * @param   player
     *          The Bukkit version of the player
     *
     * @param   sendBack
     *          If the player should be sent back, usually true when working with APIs (false is when the player leaves)
     *
     */
    @Deprecated
    public static void unregisterPlayer(@NotNull Player player, boolean sendBack) throws IOException, InvalidStatementException {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) {
            Logging.error("Player " + player.getName() + " isn't registered!");
            return;
        }
        ParkourPlayer.unregister(pp, sendBack, false, true);
    }

    /**
     * Deprecated. Use {@link dev.efnilite.witp.player.ParkourUser#unregister(ParkourUser, boolean, boolean, boolean)}.
     *
     * @param   player
     *          The player
     *
     * @param   sendBack
     *          If the player should be sent back, usually true when working with APIs (false is when the player leaves)
     */
    @Deprecated
    public static void unregisterPlayer(@NotNull ParkourPlayer player, boolean sendBack) throws IOException, InvalidStatementException {
        ParkourPlayer.unregister(player, sendBack, false, true);
    }

    /**
     * Gets a {@link ParkourPlayer} from its respective Bukkit player instance.
     *
     * @param   player
     *          The Bukkit version of the player
     *
     * @return the ParkourPlayer instance.
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        return ParkourPlayer.getPlayer(player);
    }

    /**
     * Gets a {@link ParkourUser} from a Bukkit player instance
     *
     * @param   player
     *          The player
     *
     * @return the ParkourUser instance. This may be null.
     */
    public static @Nullable ParkourUser getUser(Player player) {
        return ParkourUser.getUser(player);
    }
}
