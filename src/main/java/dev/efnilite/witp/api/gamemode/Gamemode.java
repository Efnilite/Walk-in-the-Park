package dev.efnilite.witp.api.gamemode;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Gamemode {

    String getName();

    ItemStack getItem();

    void handleItemClick(Player player, ParkourUser user, InventoryBuilder previousInventory);

}
