package dev.efnilite.ip.player.data;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.reward.RewardString;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class for storing previous data of players
 */
public class PreviousData {

    private Double health;
    private Double maxHealth;
    private InventoryData inventoryData;

    private final boolean allowFlight;
    private final boolean flying;
    private final boolean collidable;

    private final int hunger;
    private final Player player;
    private final GameMode gamemode;
    private final Location location;

    private final Collection<PotionEffect> effects;

    private final List<RewardString> rewardsLeaveList = new ArrayList<>();

    public PreviousData(@NotNull Player player) {
        this.player = player;

        gamemode = player.getGameMode();
        location = player.getLocation();
        hunger = player.getFoodLevel();

        allowFlight = player.getAllowFlight();
        flying = player.getAllowFlight();
        collidable = player.isCollidable();

        if (Option.INVENTORY_HANDLING) {
            this.inventoryData = new InventoryData(player);
            this.inventoryData.saveInventory();
            if (Option.INVENTORY_SAVING) {
                this.inventoryData.saveFile();
            }
        }

        effects = player.getActivePotionEffects();
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }

        // health handling after removing effects and inventory to avoid them affecting it
        if (Option.HEALTH_HANDLING) {
            this.health = player.getHealth();
            this.maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            player.setHealth(maxHealth);
        }
    }

    public void apply(boolean teleportBack) {
        try {
            if (teleportBack) {
                if (Option.GO_BACK) {
                    player.teleport(Option.GO_BACK_LOC);
                } else {
                    player.teleport(location);
                }
            }

            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);

            player.setAllowFlight(allowFlight);
            player.setFlying(flying);
            player.setCollidable(collidable);

            // -= Attributes =-
            if (maxHealth != null) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            if (health != null) player.setHealth(health);

            // -= Potions =-
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
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