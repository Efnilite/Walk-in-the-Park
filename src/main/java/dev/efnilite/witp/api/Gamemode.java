package dev.efnilite.witp.api;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    @NotNull ItemStack getItem(String locale);

    /**
     * When this gamemode gets clicked in the inventory screen
     *
     * @param   player
     *          The player who clicked
     *
     * @param   user
     *          The {@link ParkourUser} who clicked
     *
     * @param   previousInventory
     *          The inventory
     */
    void handleItemClick(Player player, ParkourUser user, InventoryBuilder previousInventory);

}
