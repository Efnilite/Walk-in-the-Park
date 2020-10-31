package dev.efnilite.witp.util.inventory;

import dev.efnilite.witp.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Builds inventories
 *
 * @author Efnilite
 */
@SuppressWarnings("unused")
public class InventoryBuilder {

    private int rows;
    private boolean open;
    private String name;
    private HumanEntity holder;
    private final HashMap<Integer, ItemStack> items;

    /**
     * {@link #InventoryBuilder(int, String)} but without arguments
     */
    public InventoryBuilder() {
        this(null, 1, "");
    }

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
    public InventoryBuilder(HumanEntity holder, int rows, String name) {
        this.open = false;
        this.rows = rows;
        this.name = name;
        this.holder = holder;
        this.items = new HashMap<>();
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
            holder.openInventory(inventory);
        }
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
    public InventoryBuilder setHolder(HumanEntity holder) {
        this.holder = holder;
        return this;
    }


    /**
     * Set an item in a slot
     */
    public InventoryBuilder setItem(int slot, ItemStack item) {
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
