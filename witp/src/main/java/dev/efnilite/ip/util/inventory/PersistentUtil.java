package dev.efnilite.ip.util.inventory;

import dev.efnilite.ip.IP;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * An utilities class for SuperItems
 */
public class PersistentUtil {

    /**
     * Avoid new instances
     */
    private PersistentUtil() {

    }

    /**
     * Gets persistentdata from an Itemstack
     *
     * @param   itemStack
     *          The itemstack
     *
     * @param   key
     *          The key
     *
     * @param   type
     *          The PersistentData type
     *
     * @param   <T>
     *          The type of value
     *
     * @return  the data value
     */
    public static <T> Object getPersistentData(ItemStack itemStack, String key, PersistentDataType<T, T> type) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(IP.getPlugin(), key);

        return container.get(namespacedKey, type);
    }

    /**
     * Checks if an itemstack has persistentdata
     *
     * @param   itemStack
     *          The itemstack
     *
     * @param   key
     *          The key
     *
     * @param   type
     *          The PersistentData type
     *
     * @param   <T>
     *          The type of value
     *
     * @return  true if it has the data
     */
    public static <T> boolean hasPersistentData(ItemStack itemStack, String key, PersistentDataType<T, T> type) {
        if (itemStack.getType() != Material.AIR) {
            ItemMeta meta = itemStack.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(IP.getPlugin(), key);

            return container.has(namespacedKey, type);
        } else {
            return false;
        }
    }

    /**
     * Sets the persistent data of an itemstack
     *
     * @param   itemStack
     *          The itemstack
     *
     * @param   key
     *          The key
     *
     * @param   type
     *          The PersistentData type
     *
     * @param   t
     *          The value
     *
     * @param   <T>
     *          The type of value
     */
    public static <T> void setPersistentData(ItemStack itemStack, String key, PersistentDataType<T, T> type, T t) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(IP.getPlugin(), key);

        container.set(namespacedKey, type, t);
        itemStack.setItemMeta(meta);
    }

    /**
     * Removes persistent data from an itemstack
     *
     * @param   itemStack
     *          The itemstack
     *
     * @param   key
     *          The key to be removed
     */
    public static void removePersistentData(ItemStack itemStack, String key) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(IP.getPlugin(), key);

        container.remove(namespacedKey);
        itemStack.setItemMeta(meta);
    }
}
