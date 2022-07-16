package dev.efnilite.ip.api;

import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for all Gamemodes.
 * Every registered Gamemode needs to inherit this class, because it needs identifying functions.
 */
public interface Gamemode {

    /**
     * The internal name of the Gamemode. Can be anything.
     * Make sure it doesn't match another name.
     *
     * @return the internal name used for this gamemode.
     */
    @NotNull String getName();

    /**
     * Gets the item used in menus to show this Gamemode.
     *
     * @param   locale
     *          The locale of the menu, used to adjust the name.
     *
     * @return the item.
     */
    @NotNull Item getItem(String locale);

    /**
     * Gets the {@link Leaderboard} instance that belongs to this gamemode.
     *
     * @return the {@link Leaderboard} manager that belongs to this gamemode
     */
    Leaderboard getLeaderboard();

    /**
     * Creates this Gamemode instance with a given Player.
     * For preserving {@link dev.efnilite.ip.player.data.PreviousData}, use {@link dev.efnilite.ip.player.ParkourUser#getUser(Player)}}
     *
     * @param   player
     *          The player
     */
    void create(Player player);

    /**
     * What this Gamemode should do when it is selected in a menu.
     *
     * @param   player
     *          The player who clicked.
     */
    void click(Player player);

    /**
     * Whether this mode should be visible in menus.
     *
     * @return true if yes, false if not
     */
    boolean isVisible();

}