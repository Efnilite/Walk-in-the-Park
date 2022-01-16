package dev.efnilite.witp.player.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

public class InventoryData {

    private final File file;
    private final Player player;
    @Expose
    private final HashMap<Integer, ItemStack> inventory = new HashMap<>();

    public InventoryData(Player player) {
        this.player = player;
        this.file = new File(WITP.getInstance().getDataFolder() + "/inventories", player.getUniqueId() + ".json");
    }

    /**
     * Applies the player's inventory
     */
    public void apply() {
        player.getInventory().clear();
        for (int slot : inventory.keySet()) {
            player.getInventory().setItem(slot, inventory.get(slot));
        }
    }

    public void readFile(Consumer<Boolean> successfulCallback) {
        Tasks.asyncTask(() -> {
            try {
                if (!file.exists()) {
                    successfulCallback.accept(false);
                    return;
                }
                FileReader reader = new FileReader(file);
                WITP.getGson().fromJson(reader, InventoryData.class);
                reader.close();
                successfulCallback.accept(true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Logging.stack("Error while reading inventory of " + player.getName() + " from file: ",
                        "Please report this error and the above stack trace to the developer!");
                successfulCallback.accept(false);
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
                this.inventory.put(index, item);
            }
            index++;
        }
    }
}