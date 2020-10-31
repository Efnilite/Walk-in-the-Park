package dev.efnilite.witp.util.inventory;

import dev.efnilite.witp.WITP;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Utils for PersistentData
 */
public class PersistentUtil {

    private static final Plugin plugin = WITP.getInstance();

    public static void setEntity(Entity entity, String key, String data) {
        entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, data);
    }

    public static boolean hasEntity(Entity entity, String key) {
        return entity.getPersistentDataContainer().has(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    public static String getItem(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "";
        }
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    public static String getEntity(Entity entity, String key) {
        return entity.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    public static void setItem(ItemStack item, String key, String data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, data);
        item.setItemMeta(meta);
    }

    public static void resetItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!meta.getPersistentDataContainer().isEmpty()) {
            meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "pos"));
            meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "type"));
            meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "team"));
        }
        item.setItemMeta(meta);
    }

    public static boolean hasItem(ItemStack item, String key) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }
}