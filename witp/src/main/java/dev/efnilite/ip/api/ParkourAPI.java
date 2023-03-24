package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.event.BlockGenerateEvent;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Main API accessor.
 * <ul>
 *     <li>For player joining/leaving, please view {@link ParkourUser}.</li>
 *     <li>For player settings, please view {@link ParkourPlayer}.</li>
 *     <li>For player spectating, please view {@link dev.efnilite.ip.player.ParkourSpectator}.</li>
 *     <li>For events, please view {@link BlockGenerateEvent} and others in the events package.</li>
 *     <li>For Sessions, please view {@link dev.efnilite.ip.session.Session}.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class ParkourAPI {

    private ParkourAPI() throws IllegalAccessException {
        throw new IllegalAccessException("Initializing API class");
    }

    /**
     * @return The {@link Registry} class
     */
    public static Registry getRegistry() {
        return IP.getRegistry();
    }

    /**
     * Gets a {@link ParkourPlayer} from its respective Bukkit player instance.
     *
     * @param player The Bukkit version of the player
     * @return the ParkourPlayer instance.
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        return ParkourPlayer.getPlayer(player);
    }

    /**
     * Gets a {@link ParkourUser} from a Bukkit player instance
     *
     * @param player The player
     * @return The requested ParkourUser instance. This may be null.
     */
    public static @Nullable ParkourUser getUser(Player player) {
        return ParkourUser.getUser(player);
    }
}
