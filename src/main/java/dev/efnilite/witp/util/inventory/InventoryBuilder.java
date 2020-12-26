package dev.efnilite.witp.util.inventory;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * Builds inventories
 *
 * @author Efnilite
 */
public class InventoryBuilder implements Listener {

    private final ParkourPlayer pp;

    private int rows;
    private boolean open;
    private String name;
    private Player holder;
    private UUID uuid;
    private final HandlerList handlerList;
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
    public InventoryBuilder(@Nullable ParkourPlayer pp, int rows, String name) {
        this.open = false;
        this.uuid = null;
        this.rows = rows;
        this.name = name;
        this.pp = pp;
        if (pp != null) {
            this.holder = pp.getPlayer();
        }
        this.handlerList = new HandlerList();
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
        uuid = UUID.randomUUID();
        if (open) {
            if (pp == null) {
                Verbose.error("Tried opening inventory " + uuid.toString() + " but player is null");
            } else {
                holder.openInventory(inventory);
                pp.openInventory = uuid;
            }
        }
        unregister();
        Bukkit.getPluginManager().registerEvents(this, WITP.getInstance());
        return inventory;
    }

    /**
     * When finished building, should the inventory be opened?
     */
    public InventoryBuilder open() {
        this.open = Boolean.parseBoolean(Util.reverseBoolean("" + this.open));
        return this;
    }

    /**
     * Set the holder
     */
    public InventoryBuilder setHolder(Player holder) {
        this.holder = holder;
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

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (player.getOpenInventory().getTitle().equals(name) && ParkourPlayer.getPlayer(player) != null) {
            pp.openInventory = null;
            unregister();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (player.getOpenInventory().getTitle().equals(name) && ParkourPlayer.getPlayer(player) != null && current != null && uuid == pp.openInventory) {
            event.setCancelled(true);
            InventoryConsumer consumer = onClick.get(event.getSlot());
            if (consumer != null) {
                consumer.accept(event, current);
            }
        }
    }

    /**
     * Returns the UUID for this inventory
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Unregisters this thing
     */
    public void unregister() {
        handlerList.unregister(this);
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
     * Gets the holder
     */
    public InventoryHolder getHolder() {
        return holder;
    }

    /**
     * Gets the name
     */
    public String getName() {
        return name;
    }
}
