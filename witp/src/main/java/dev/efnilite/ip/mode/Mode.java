package dev.efnilite.ip.mode;

import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for all modes.
 * Every registered mode needs to inherit this class, because it needs identifying functions.
 */
public interface Mode {

    /**
     * Method that gets called when a mode is clicked in the menu or joined using /parkour join.
     *
     * @param player The player.
     */
    void create(Player player);

    /**
     * @param locale The locale of the menu, used to adjust the name.
     * @return The item used in menus to show this mode. If this item is null, the mode won't be displayed.
     */
    @Nullable Item getItem(String locale);

    /**
     * @return The {@link Leaderboard} that belongs to this mode
     */
    @Nullable Leaderboard getLeaderboard();

    /**
     * @return The internal name used for this mode.
     */
    @NotNull String getName();

}