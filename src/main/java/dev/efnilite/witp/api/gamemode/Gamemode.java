package dev.efnilite.witp.api.gamemode;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Gamemode {

    @NotNull String getName();

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
