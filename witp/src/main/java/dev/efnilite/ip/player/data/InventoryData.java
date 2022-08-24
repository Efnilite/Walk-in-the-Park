package dev.efnilite.ip.player.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.serialization.ObjectSerializer;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
        this.file = new File(IP.getPlugin().getDataFolder() + "/inventories", player.getUniqueId() + ".json");
    }

    /**
     * Applies the player's inventory
     */
    public boolean apply(boolean readFromFile) {
        player.getInventory().clear();
        boolean wasSuccessful = true;

        for (int slot : inventory.keySet()) {
            if (readFromFile) {
                ItemStack item = ObjectSerializer.deserialize64(inventory.get(slot));
                if (item == null) {
                    wasSuccessful = false;
                    continue;
                }

                player.getInventory().setItem(slot, item);
            } else {
                player.getInventory().setItem(slot, loadedInventory.get(slot));
            }
        }

        return wasSuccessful;
    }

    public void readFile(Consumer<@Nullable InventoryData> successfulCallback) {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try {
                        if (!file.exists()) {
                            successfulCallback.accept(null);
                            return;
                        }
                        FileReader reader = new FileReader(file);
                        InventoryData data = IP.getGson().fromJson(reader, InventoryData.class);
                        data.player = player;
                        data.file = file;
                        successfulCallback.accept(data);

                        reader.close();
                    } catch (IOException ex) {
                        IP.logging().stack("Error while reading inventory of " + player.getName() + " from file " + file.getName(), ex);
                        successfulCallback.accept(null);
                    }
                })
                .run();
    }

    public void saveFile() {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try {
                        if (!file.exists()) {
                            File folder = new File(IP.getPlugin().getDataFolder() + "/inventories");
                            if (!folder.exists()) {
                                folder.mkdirs();
                            }
                            file.createNewFile();
                        }
                        FileWriter writer = new FileWriter(file);
                        IP.getGson().toJson(this, writer);
                        writer.flush();
                        writer.close();
                    } catch (IOException ex) {
                        IP.logging().stack("Error while saving inventory of " + player.getName() + " to file " + file.getName(), ex);
                    }
                })
                .run();
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     */
    public void saveInventory() {
        int index = 0;
        Inventory inventory = this.player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                this.inventory.put(index, ObjectSerializer.serialize64(item));
                this.loadedInventory.put(index, item);
            }
            index++;
        }

        String command = Option.ALT_INVENTORY_SAVING_COMMAND;
        if (command != null && command.length() > 0) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }
}