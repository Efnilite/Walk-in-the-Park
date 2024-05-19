package dev.efnilite.ip.api;

import dev.efnilite.ip.api.event.ParkourBlockGenerateEvent;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourPlayer2;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Main API handler.
 * <ul>
 *     <li>For general player handling, please view {@link dev.efnilite.ip.player.ParkourPlayer2}.</li>
 *     <li>For parkour players, please view {@link ParkourPlayer}.</li>
 *     <li>For events, please view {@link ParkourBlockGenerateEvent} and others in the events package.</li>
 *     <li>For Sessions, please view {@link dev.efnilite.ip.session.Session}.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class ParkourAPI {

    private ParkourAPI() throws IllegalAccessException {
        throw new IllegalAccessException("Initializing API class");
    }

    /**
     * @param player The player.
     * @return The {@link ParkourPlayer2}. Null if not found.
     */
    public static @Nullable ParkourPlayer2 getPlayer(Player player) {
        return ParkourPlayer2.as(player);
    }
}
