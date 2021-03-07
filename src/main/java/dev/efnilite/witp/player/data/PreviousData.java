package dev.efnilite.witp.player.data;

import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class PreviousData {

    public HashMap<Integer, ItemStack> previousInventory;
    public final GameMode previousGamemode;
    public final Location previousLocation;

    public PreviousData(Player player) {
        saveInventory(player);
        this.previousGamemode = player.getGameMode();
        this.previousLocation = player.getLocation();
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     */
    protected void saveInventory(Player player) {
        this.previousInventory = new HashMap<>();
        if (Option.INVENTORY_HANDLING) {
            int index = 0;
            Inventory inventory = player.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (item != null) {
                    previousInventory.put(index, item);
                }
                index++;
            }
        }
    }
}
