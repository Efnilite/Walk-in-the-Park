package dev.efnilite.witp.player.data;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Version;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class PreviousData {

    private double health;
    private double maxHealth;
    private InventoryData data;
    private final int hunger;
    private final Player player;
    private final GameMode gamemode;
    private final Location location;

    public PreviousData(@NotNull Player player) {
        this.player = player;
        this.gamemode = player.getGameMode();
        this.location = player.getLocation();
        this.hunger = player.getFoodLevel();

        if (Option.SAVE_STATS.get()) {
            this.health = player.getHealth();
            this.maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        }
        if (Option.HEALTH_HANDLING.get()) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        }
        if (Option.INVENTORY_HANDLING.get()) {
            this.data = new InventoryData(player);
            this.data.saveInventory();
            if (Option.INVENTORY_SAVING.get()) {
                this.data.saveFile();
            }
        }
        if (Version.isHigherOrEqual(Version.V1_13)) {
            for (PotionEffectType value : PotionEffectType.values()) {
                player.removePotionEffect(value);
            }
        }
    }

    public void apply(boolean teleportBack) {
        try {
            if (teleportBack) {
                if (Option.GO_BACK.get()) {
                    Location to = Util.parseLocation(WITP.getConfiguration().getString("config", "bungeecord.go-back"));
                    player.teleport(to);
                } else {
                    player.teleport(location);
                }
            }

            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);

            // -= Attributes =-
            if (Option.SAVE_STATS.get() && Option.HEALTH_HANDLING.get()) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                player.setHealth(health);
            }
        } catch (Throwable ex) {// not optimal but there isn't another way
            ex.printStackTrace();
            Logging.stack("Error while recovering stats of " + player.getName() + ": " + ex.getMessage(),
                    "Please report this error to the developer! Inventory will still be restored.");
        }
        if (data != null) {
            data.apply(false);
        }
    }
}
