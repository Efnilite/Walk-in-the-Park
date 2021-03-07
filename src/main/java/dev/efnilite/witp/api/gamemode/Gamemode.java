package dev.efnilite.witp.api.gamemode;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Gamemode {

    @NotNull String getName();

    @NotNull ItemStack getItem(String locale);

    void handleItemClick(Player player, ParkourUser user, InventoryBuilder previousInventory);

}
