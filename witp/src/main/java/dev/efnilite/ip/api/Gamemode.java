package dev.efnilite.ip.api;

import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
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
     * When this gamemode gets clicked in the inventory screen
     *
     * @param   player
     *          The player who clicked
     *
     * @param   user
     *          The {@link ParkourUser} who clicked
     *
     * @param   previousMenu
     *          The inventory
     */
    void handleItemClick(Player player, ParkourUser user, Menu previousMenu);

    boolean isMultiplayer();

}