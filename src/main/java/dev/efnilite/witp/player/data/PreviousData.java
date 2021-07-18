package dev.efnilite.witp.player.data;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class PreviousData {

    public HashMap<Integer, ItemStack> inventory = new HashMap<>();
    public double speed;
    public final GameMode gamemode;
    public final Location location;
    public int hunger;
    public double health;
    public Player player;

    public PreviousData(@NotNull Player player) {
        this.player = player;
        this.gamemode = player.getGameMode();
        this.location = player.getLocation();
        this.hunger = player.getFoodLevel();
        this.health = player.getHealth();
        saveInventory();
        speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
        
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
        for (PotionEffectType value : PotionEffectType.values()) {
            player.removePotionEffect(value);
        }
    }

    public void apply() {
        if (Option.GO_BACK) {
            Location to = Util.parseLocation(WITP.getConfiguration().getString("config", "bungeecord.go-back"));
            player.teleport(to);
        } else {
            player.teleport(location);
        }
        player.setHealth(health);
        player.setFoodLevel(hunger);
        player.setGameMode(gamemode);
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        if (Option.INVENTORY_HANDLING) {
            player.getInventory().clear();
            for (int slot : inventory.keySet()) {
                player.getInventory().setItem(slot, inventory.get(slot));
            }
        }
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     */
    protected void saveInventory() {
        if (Option.INVENTORY_HANDLING) {
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
}
