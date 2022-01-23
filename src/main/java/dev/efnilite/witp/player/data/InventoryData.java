package dev.efnilite.witp.player.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class InventoryData {

    private File file;
    private Player player;
    private final HashMap<Integer, ItemStack> loadedInventory = new HashMap<>();
    @Expose
    private final HashMap<Integer, String> inventory = new HashMap<>();

    public InventoryData(Player player) {
        this.player = player;
        this.file = new File(WITP.getInstance().getDataFolder() + "/inventories", player.getUniqueId() + ".json");
    }

    /**
     * Applies the player's inventory
     */
    public boolean apply(boolean readFromFile) {
        player.getInventory().clear();
        boolean wasSuccesful = true;

        for (int slot : inventory.keySet()) {
            if (readFromFile) {
                ItemStack item = deserialize64(inventory.get(slot));
                if (item == null) {
                    wasSuccesful = false;
                    continue;
                }
                player.getInventory().setItem(slot, item);
            } else {
                player.getInventory().setItem(slot, loadedInventory.get(slot));
            }
        }

        return wasSuccesful;
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
                Logging.stack("Error while reading inventory of " + player.getName() + " from file: ",
                        "Please report this error and the above stack trace to the developer!", ex);
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
                Logging.stack("Error while saving inventory of " + player.getName() + " to file: ",
                        "Please report this error and the above stack trace to the developer!", ex);
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
                this.inventory.put(index, serialize64(item));
                this.loadedInventory.put(index, item);
            }
            index++;
        }
        String command = Option.ALT_INVENTORY_SAVING_COMMAND.get();
        if (command != null && command.length() > 0) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    public static String serialize64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream output = new BukkitObjectOutputStream(outputStream);

            output.writeObject(item);

            output.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Throwable throwable) {
            Logging.stack("There was an error while trying to convert an item to base 64!",
                    "Please retry. If this error still persists, contact the developer!", throwable);
            return "";
        }
    }

    public @Nullable ItemStack deserialize64(String string) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(string));
            BukkitObjectInputStream input = new BukkitObjectInputStream(inputStream);

            input.close();
            return (ItemStack) input.readObject();
        } catch (Throwable throwable) {
            Logging.stack("There was an error while trying to convert an item from base 64!",
                    "You are probably using an outdated inventory saving format. Please delete file " + file, throwable);
            return null;
        }
    }

//    private ItemStack deserialize(Map<String, Object> serialized) {
//        if (serialized.get("type") != null) {
//            Logging.stack("You are using an outdated or incompatible inventory file", "Delete file " + file + " and try again!");
//            return new ItemStack(Material.STONE);
//        }
//
//        Number amount = 0D;
//        Material material = Material.getMaterial(String.valueOf(serialized.get("material"))); // saved as String
//
//        if (material == null) {
//            Logging.error("Inventory recovery material not found: " + serialized.get("material"));
//            material = Material.STONE;
//        }
//
//        if (serialized.containsKey("amount")) {
//            amount = (Number) serialized.get("amount"); // saved as Integer but the parser hates me
//        }
//
//        ItemStack item = new ItemStack(material, amount.intValue());
//        ItemMeta meta = item.getItemMeta();
//        assert meta != null;
//
//        if (serialized.containsKey("name")) {
//            meta.setDisplayName(String.valueOf(serialized.get("name")));
//        }
//        Object raw;
//        if (serialized.containsKey("lore")) {
//            raw = serialized.get("lore");
//            if (raw instanceof List) {
//                List<?> list = (List<?>) raw;
//                List<String> lore = new ArrayList<>();
//                for (Object o : list) {
//                    lore.add(String.valueOf(o));
//                }
//                meta.setLore(lore);
//            }
//        }
//
//        if (serialized.containsKey("enchantments")) {
//            raw = serialized.get("enchantments");
//            if (raw instanceof Map) {
//                Map<?, ?> map = (Map<?, ?>) raw;
//                for (Object o : map.keySet()) {
//                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(String.valueOf(o)));
//                    if (enchantment == null) {
//                        continue;
//                    }
//                    double level = (double) map.get(o);
//                    item.addEnchantment(enchantment, (int) level);
//                }
//            }
//        }
//
//        if (serialized.containsKey("damage") && meta instanceof Damageable) {
//            Double value = (Double) serialized.get("damage");
//            ((Damageable) meta).setDamage(value.intValue()); // saved as Integer
//        }
//
//        item.setItemMeta(meta);
//        return item;
//    }
//
//    private Map<String, Object> serialize(ItemStack item) {
//        Map<String, Object> result = new HashMap<>();
//
//        ItemMeta meta = item.getItemMeta();
//
//        Map<String, Integer> serEnchants = new HashMap<>();  // SOMEHOW ItemStack#serialize doesn't support enchantments
//        Map<Enchantment, Integer> enchants = item.getEnchantments();
//        for (Enchantment enchantment : enchants.keySet()) {
//            serEnchants.put(enchantment.getKey().toString(), enchants.get(enchantment)); // ItemStack#deserialize uses name value instead of key
//        }
//
//        result.put("material", item.getType().name());
//        result.put("amount", item.getAmount());
//
//        if (meta != null && !Bukkit.getItemFactory().equals(meta, null)) {
//            if (meta.hasDisplayName()) {
//                result.put("name", meta.getDisplayName());
//            }
//            if (meta.hasLore()) {
//                result.put("lore", meta.getLore());
//            }
//        }
//        if (!enchants.isEmpty()) {
//            result.put("enchantments", serEnchants);
//        }
//        if (item.getType().getMaxDurability() != 0 && meta instanceof Damageable) {
//            result.put("damage", ((Damageable) meta).getDamage());
//        }
//        return result;
//    }
}