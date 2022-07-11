package dev.efnilite.ip.player.data;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.reward.RewardString;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.util.Version;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing previous data of players
 */
public class PreviousData {

    private double health;
    private double maxHealth;
    private InventoryData inventoryData;
    private final int hunger;
    private final Player player;
    private final GameMode gamemode;
    private final Location location;

    private final List<RewardString> rewardsLeaveList = new ArrayList<>();

    public PreviousData(@NotNull Player player) {
        this.player = player;
        this.gamemode = player.getGameMode();
        this.location = player.getLocation();
        this.hunger = player.getFoodLevel();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (Option.SAVE_STATS.get()) {
            this.health = player.getHealth();
            this.maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        }

        if (Option.HEALTH_HANDLING.get()) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        }

        if (Option.INVENTORY_HANDLING.get()) {
            this.inventoryData = new InventoryData(player);
            this.inventoryData.saveInventory();
            if (Option.INVENTORY_SAVING.get()) {
                this.inventoryData.saveFile();
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
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }

            if (teleportBack) {
                if (Option.GO_BACK.get()) {
                    Location to = Util.parseLocation(IP.getConfiguration().getString("config", "bungeecord.go-back"));
                    player.teleport(to);
                } else {
                    player.teleport(location);
                }
            }

            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);
            player.setFlying(false);
            player.setInvisible(false);

            // -= Attributes =-
            if (Option.SAVE_STATS.get() && Option.HEALTH_HANDLING.get()) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                player.setHealth(health);
            }
        } catch (Throwable ex) {// not optimal but there isn't another way
            IP.logging().stack("Error while recovering stats of " + player.getName(), ex);
        }
        if (inventoryData != null) {
            inventoryData.apply(false);
        }
    }

    /**
     * Adds a reward to the leave list
     *
     * @param   string
     *          The reward to give on leave
     */
    public void addReward(RewardString string) {
        rewardsLeaveList.add(string);
    }

    /**
     * Applies the rewards stored in the leave list
     *
     * @param   player
     *          The player to apply these rewards to
     */
    public void giveRewards(@NotNull ParkourPlayer player) {
        rewardsLeaveList.forEach(s -> s.execute(player));
    }
}