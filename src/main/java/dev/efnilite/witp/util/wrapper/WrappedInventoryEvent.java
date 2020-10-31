package dev.efnilite.witp.util.wrapper;

import dev.efnilite.witp.ParkourPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * When someone clicks in an inventory
 */
@SuppressWarnings("unused")
public class WrappedInventoryEvent extends EventWrapper implements Cancellable {

    private final int slot;
    private final Player player;
    private final String name;
    private final Inventory inventory;
    private final ItemStack item;
    private final ItemMeta meta;
    private final ClickType clickType;

    /**
     * Constructor
     *
     * @param player The player who clicked the inventory
     * @param name The inventory name
     * @param inventory The clicked inventory
     * @param item The clicked item
     * @param meta The meta of the clicked item
     * @param clickType The click type
     * @param slot The slot
     */
    public WrappedInventoryEvent(Player player, String name, Inventory inventory, ItemStack item, ItemMeta meta, ClickType clickType, int slot) {
        this.player = player;
        this.name = name;
        this.inventory = inventory;
        this.item = item;
        this.meta = meta;
        this.clickType = clickType;
        this.slot = slot;
    }

    /**
     * Gets the slot
     */
    public int getSlot() {
        return slot;
    }

    public ParkourPlayer getPlayer() {
        return ParkourPlayer.getPlayer(player);
    }

    /**
     * Gets the player
     */
    public Player getBukkitPlayer() {
        return player;
    }

    /**
     * Gets the click type
     */
    public ClickType getClickType() {
        return clickType;
    }

    /**
     * Gets the inventory name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the clicked inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Gets the clicked item
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the clicked item's item meta
     */
    public ItemMeta getMeta() {
        return meta;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
