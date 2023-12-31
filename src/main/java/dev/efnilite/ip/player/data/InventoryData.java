package dev.efnilite.ip.player.data;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class InventoryData {

    private final File file;
    private final Player player;
    private Map<Integer, ItemStack> items = new HashMap<>();

    public InventoryData(Player player) {
        this.player = player;
        this.file = IP.getInFolder("inventories/%s".formatted(player.getUniqueId()));
    }

    /**
     * Gives all items to the player.
     */
    public void apply() {
        player.getInventory().clear();
        items.forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    /**
     * Loads inventory data from file.
     *
     * @param onFinish What to do when the async procedure has finished.
     */
    public void load(Consumer<@Nullable InventoryData> onFinish) {
        if (!file.exists()) {
            onFinish.accept(null);
            return;
        }

        Task.create(IP.getPlugin()).async().execute(() -> loadFile(onFinish)).run();
    }

    @SuppressWarnings("unchecked")
    private void loadFile(Consumer<@Nullable InventoryData> onFinish) {
        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            items = (Map<Integer, ItemStack>) stream.readObject();

            onFinish.accept(this);
        } catch (IOException | ClassNotFoundException ex) {
            IP.logging().stack("Error while reading inventory of %s from file %s".formatted(player.getName(), file.getName()), ex);
            onFinish.accept(null);
        }
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     *
     * @param toFile Whether the file should be updated.
     */
    public void save(boolean toFile) {
        int index = 0;

        Inventory inventory = this.player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                this.items.put(index, item);
            }

            index++;
        }

        String command = Option.ALT_INVENTORY_SAVING_COMMAND;
        if (command != null && !command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        if (toFile) {
            Task.create(IP.getPlugin()).async().execute(this::saveFile).run();
        }
    }

    private void saveFile() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException ex) {
            IP.logging().stack("Error while creating file to save inventory of %s to file %s".formatted(player.getName(), file.getName()), ex);
        }

        try (ObjectOutputStream stream = new BukkitObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(items);
            stream.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while saving inventory of %s to file %s".formatted(player.getName(), file.getName()), ex);
        }
    }
}