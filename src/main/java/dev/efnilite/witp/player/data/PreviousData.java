package dev.efnilite.witp.player.data;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.Version;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class PreviousData {

    private double speed;
    private double health;
    private double maxHealth;
    private final int hunger;
    private final GameMode gamemode;
    private final Location location;
    private final Player player;
    private final HashMap<Integer, ItemStack> inventory = new HashMap<>();

    public PreviousData(@NotNull Player player) {
        this.player = player;
        this.gamemode = player.getGameMode();
        this.location = player.getLocation();
        this.hunger = player.getFoodLevel();

        if (Option.SAVE_STATS) {
            this.health = player.getHealth();
            this.maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        }
        saveInventory();
//        speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
        
//        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1); unsure whether this is the actual value

        // Set to defaults

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);

        if (Version.isHigherOrEqual(Version.V1_13)) {
            for (PotionEffectType value : PotionEffectType.values()) {
                player.removePotionEffect(value);
            }
        }
    }

    public void apply() {
        try {
            if (Option.GO_BACK) {
                Location to = Util.parseLocation(WITP.getConfiguration().getString("config", "bungeecord.go-back"));
                player.teleport(to);
            } else {
                player.teleport(location);
            }
            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);

            // -= Attributes =-
            if (Option.SAVE_STATS) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                player.setHealth(health);
            }
            //        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        } catch (Exception ex) {// not optimal but there isn't another way
            ex.printStackTrace();
            Verbose.error("Error while giving the stats of player " + player.getName() + " back! The inventory will still be restored.");
        }
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
