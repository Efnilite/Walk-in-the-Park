package dev.efnilite.ip.api;

import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for all Gamemodes.
 * Every registered Gamemode needs to inherit this class, because it needs identifying functions.
 */
public interface Mode {

    /**
     * @return The internal name used for this gamemode.
     */
    @NotNull String getName();

    /**
     * @param locale The locale of the menu, used to adjust the name.
     * @return The item used in menus to show this mode.
     */
    @NotNull Item getItem(String locale);

    /**
     * @return The {@link Leaderboard} that belongs to this mode
     */
    @Nullable Leaderboard getLeaderboard();

    /**
     * Creates this Gamemode instance with a given Player.
     * For preserving {@link dev.efnilite.ip.player.data.PreviousData}, use {@link dev.efnilite.ip.player.ParkourUser#getUser(Player)}}
     *
     * @param player The player
     */
    void create(Player player);

    /**
     * What this Gamemode should do when it is selected in a menu.
     *
     * @param player The player who clicked.
     */
    void click(Player player);

    /**
     * @return True if this mode should be visible in menus, false if not.
     */
    boolean isVisible();

}