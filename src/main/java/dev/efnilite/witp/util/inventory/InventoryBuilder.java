package dev.efnilite.witp.util.inventory;

import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Builds inventories
 *
 * @author Efnilite
 */
public class InventoryBuilder {

    private int rows;
    private boolean open;
    private String name;
    private final ParkourUser player;
    private final HashMap<Integer, ItemStack> items;
    private final HashMap<Integer, InventoryConsumer> onClick;

    /**
     * New instance for builder
     *
     * @param   rows
     *          amount of rows
     * @param   name
     *          the name ('\&' can be used)
     */
    public InventoryBuilder(int rows, String name) {
        this(null, rows, name);
    }

    /**
     * {@link #InventoryBuilder(int, String)} but with the holder
     */
    public InventoryBuilder(@Nullable ParkourUser player, int rows, String name) {
        this.open = false;
        this.rows = rows;
        this.name = name;
        this.player = player;
        this.items = new HashMap<>();
        this.onClick = new HashMap<>();
    }

    /**
     * Builds the inventory
     */
    public Inventory build() {
        Inventory inventory = Bukkit.createInventory(null, rows * 9, ChatColor.translateAlternateColorCodes('&', name));
        for (int slot : items.keySet()) {
            inventory.setItem(slot, items.get(slot));
        }
        if (open) {
            if (player == null) {
                Verbose.error("Tried opening inventory but player is null");
            } else {
                player.getPlayer().openInventory(inventory);
                player.openInventory = new OpenInventoryData(name, onClick);
            }
        }
        return inventory;
    }

    /**
     * When finished building, should the inventory be opened?
     */
    public InventoryBuilder open() {
        this.open = !open;
        return this;
    }

    /**
     * Set an item in a slot
     */
    public InventoryBuilder setItem(int slot, ItemStack item, @Nullable InventoryConsumer onClick) {
        this.onClick.put(slot, onClick);
        this.items.put(slot, item);
        return this;
    }

    /**
     * Sets the amount of rows
     */
    public InventoryBuilder setRows(int rows) {
        this.rows = rows;
        return this;
    }

    /**
     * Sets the inventory name
     */
    public InventoryBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get an item from a slot
     */
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Gets the rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the name
     */
    public String getName() {
        return name;
    }

    public static class OpenInventoryData {

        public final String name;
        public final HashMap<Integer, InventoryConsumer> itemData;

        public OpenInventoryData(String name, HashMap<Integer, InventoryConsumer> onClick) {
            this.name = name;
            this.itemData = onClick;
        }
    }

    public static class ClickHandler implements Listener {

        public ClickHandler(Plugin plugin) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getPlayer() instanceof Player) {
                ParkourUser user = ParkourUser.getUser((Player) event.getPlayer());
                if (user != null) {
                    user.openInventory = null;
                }
            }
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (event.getWhoClicked() instanceof Player) {
                ParkourUser user = ParkourUser.getUser((Player) event.getWhoClicked());
                if (user != null) {
                    OpenInventoryData data = user.openInventory;
                    if (data != null) {
                        event.setCancelled(true);
                        InventoryConsumer consumer = data.itemData.get(event.getSlot());
                        if (consumer != null) {
                            consumer.accept(event, event.getCurrentItem());
                        }

                    }
                }
            }
        }
    }
}
