package dev.efnilite.witp.player.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InventoryData {

    private File file;
    private Player player;
    private final HashMap<Integer, ItemStack> loadedInventory = new HashMap<>();
    @Expose
    private final HashMap<Integer, Map<String, Object>> inventory = new HashMap<>();

    public InventoryData(Player player) {
        this.player = player;
        this.file = new File(WITP.getInstance().getDataFolder() + "/inventories", player.getUniqueId() + ".json");
    }

    /**
     * Applies the player's inventory
     */
    public void apply(boolean readFromFile) {
        player.getInventory().clear();
        for (int slot : inventory.keySet()) {
            if (readFromFile) {
                player.getInventory().setItem(slot, deserialize(inventory.get(slot)));
            } else {
                player.getInventory().setItem(slot, loadedInventory.get(slot));
            }
        }
    }

    public void readFile(Consumer<@Nullable InventoryData> successfulCallback) {
        Tasks.asyncTask(() -> {
            try {
                if (!file.exists()) {
                    successfulCallback.accept(null);
                    return;
                }
                FileReader reader = new FileReader(file);
                InventoryData data = WITP.getGson().fromJson(reader, InventoryData.class);
                data.player = player;
                data.file = file;
                successfulCallback.accept(data);

                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Logging.stack("Error while reading inventory of " + player.getName() + " from file: ",
                        "Please report this error and the above stack trace to the developer!");
                successfulCallback.accept(null);
            }
        });
    }

    public void saveFile() {
        Tasks.asyncTask(() -> {
            try {
                if (!file.exists()) {
                    File folder = new File(WITP.getInstance().getDataFolder() + "/inventories");
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    file.createNewFile();
                }
                FileWriter writer = new FileWriter(file);
                WITP.getGson().toJson(this, writer);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Logging.stack("Error while saving inventory of " + player.getName() + " to file: ",
                        "Please report this error and the above stack trace to the developer!");
            }
        });
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     */
    public void saveInventory() {
        int index = 0;
        Inventory inventory = this.player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                this.inventory.put(index, serialize(item));
                this.loadedInventory.put(index, item);
            }
            index++;
        }
    }

    private ItemStack deserialize(Map<String, Object> serialized) {
        if (serialized.get("type") != null) {
            Logging.stack("You are using an outdated or incompatible inventory file", "Delete file '" + file + "' and try again!");
            return new ItemStack(Material.STONE);
        }

        Number amount = 0D;
        Material material = Material.getMaterial(String.valueOf(serialized.get("material"))); // saved as String

        if (material == null) {
            Logging.error("Inventory recovery material not found: " + serialized.get("material"));
            material = Material.STONE;
        }

        if (serialized.containsKey("amount")) {
            amount = (Number) serialized.get("amount"); // saved as Integer but the parser hates me
        }

        ItemStack item = new ItemStack(material, amount.intValue());
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        if (serialized.containsKey("name")) {
            meta.setDisplayName(String.valueOf(serialized.get("name")));
        }
        Object raw;
        if (serialized.containsKey("lore")) {
            raw = serialized.get("lore");
            if (raw instanceof List) {
                List<?> list = (List<?>) raw;
                List<String> lore = new ArrayList<>();
                for (Object o : list) {
                    lore.add(String.valueOf(o));
                }
                meta.setLore(lore);
            }
        }

        if (serialized.containsKey("enchantments")) {
            raw = serialized.get("enchantments");
            if (raw instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) raw;
                for (Object o : map.keySet()) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(String.valueOf(o)));
                    if (enchantment == null) {
                        continue;
                    }
                    double level = (double) map.get(o);
                    item.addEnchantment(enchantment, (int) level);
                }
            }
        }

        if (serialized.containsKey("damage") && meta instanceof Damageable) {
            Double value = (Double) serialized.get("damage");
            ((Damageable) meta).setDamage(value.intValue()); // saved as Integer
        }

        item.setItemMeta(meta);
        return item;
    }

    private Map<String, Object> serialize(ItemStack item) {
        Map<String, Object> result = new HashMap<>();

        ItemMeta meta = item.getItemMeta();

        Map<String, Integer> serEnchants = new HashMap<>();  // SOMEHOW ItemStack#serialize doesn't support enchantments
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        for (Enchantment enchantment : enchants.keySet()) {
            serEnchants.put(enchantment.getKey().toString(), enchants.get(enchantment)); // ItemStack#deserialize uses name value instead of key
        }

        result.put("material", item.getType().name());
        result.put("amount", item.getAmount());

        if (meta != null && !Bukkit.getItemFactory().equals(meta, null)) {
            if (meta.hasDisplayName()) {
                result.put("name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                result.put("lore", meta.getLore());
            }
        }
        if (!enchants.isEmpty()) {
            result.put("enchantments", serEnchants);
        }
        if (item.getType().getMaxDurability() != 0 && meta instanceof Damageable) {
            result.put("damage", ((Damageable) meta).getDamage());
        }
        return result;
    }
}