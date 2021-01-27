package dev.efnilite.witp.util.inventory;

import dev.efnilite.witp.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A class for creating items.
 *
 * @author Efnilite
 */
public class ItemBuilder {

    private int amount;
    private int durability;
    private boolean unbreakable;
    private String name;
    private final List<String> lore;
    private Material type;
    private final HashMap<String, String> data;

    /**
     * Creates a new instance
     */
    public ItemBuilder() {
        this(null, null);
    }

    /**
     * Creates a new instance
     *
     * @param   material
     *          The material
     *
     * @param   name
     *          The name of the item
     */
    public ItemBuilder(Material material, String name) {
        this(material, 1, name);
    }

    /**
     * Creates a new instance
     *
     * @param   material
     *          The material
     *
     * @param   amount
     *          The amount of the item
     *
     * @param   name
     *          The name of the item
     */
    public ItemBuilder(Material material, int amount, String name) {
        this.amount = amount;
        if (material != null) {
            this.durability = material.getMaxDurability();
        } else {
            material = Material.GRASS_BLOCK;
        }
        this.name = name;
        this.lore = new ArrayList<>();
        this.data = new HashMap<>();
        this.type = material;
        this.unbreakable = false;
    }

    /**
     * Finishes everything and gives the ItemStack result.
     *
     * @return the result
     */
    public ItemStack build() {
        ItemStack item = new ItemStack(type, amount);
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        assert meta != null;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore);
        ((Damageable) meta).setDamage(Math.abs(durability - type.getMaxDurability()));
        meta.setUnbreakable(unbreakable);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * The same as {@link #build()}} but with PersistentData added (due to Plugin param)
     *
     * @return the same as {@link #build()} but with PersistentData
     */
    public ItemStack buildPersistent(Plugin plugin) {
        ItemStack item = build();
        ItemMeta meta = item.getItemMeta();
        for (String d : data.keySet()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, d), PersistentDataType.STRING, data.get(d));
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Sets the durability of the item
     */
    public ItemBuilder setDurability(int durability) {
        this.durability = durability;
        return this;
    }

    /**
     * Adds PersistentData
     *
     * @param   data
     *          The data to be added
     *
     * @return the instancec
     */
    public ItemBuilder setPersistentData(String key, String data) {
        this.data.put(key, data);
        return this;
    }

    /**
     * Set unbreakable
     *
     * @return the instance
     */
    public ItemBuilder unbreakable() {
        this.unbreakable = true;
        return this;
    }

    /**
     * Set glowing
     *
     * @return the instance
     */
    public ItemBuilder glowing(boolean predicate) {
        if (predicate) {
        }
        return this;
    }

    /**
     * Set glowing
     *
     * @return the instance
     */
    public ItemBuilder glowing() {
        return this;
    }

    /**
     * Sets the item amount
     *
     * @param   amount
     *          The item amount
     *
     * @return  the instance
     */
    public ItemBuilder setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Sets the lore
     *
     * @param   lore
     *          The lore
     *
     * @return  the instance
     */
    public ItemBuilder setLore(@Nullable List<String> lore) {
        if (lore == null || lore.size() == 0) {
            return this;
        }
        for (String l : lore) {
            this.lore.add(Util.color(l));
        }
        return this;
    }

    /**
     * Sets the lore
     *
     * @param   lore
     *          The lore
     *
     * @return the instance
     */
    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    /**
     * Sets the name
     *
     * @param   name
     *          The name
     *
     * @return  the instance
     */
    public ItemBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the type
     *
     * @param   type
     *          The type
     *
     * @return  the instance
     */
    public ItemBuilder setType(Material type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the amount
     *
     * @return the amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Gets the lore
     *
     * @return the lore
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Gets the item type
     *
     * @return the type
     */
    public Material getType() {
        return type;
    }

    /**
     * Gets the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
}
