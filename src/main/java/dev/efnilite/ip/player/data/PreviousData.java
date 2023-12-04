package dev.efnilite.ip.player.data;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
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

    private final int hunger;
    private final boolean flying;
    private final boolean allowFlight;
    private final GameMode gamemode;
    private final Location location;
    private final Collection<PotionEffect> effects;

    /**
     * List of all {@link RewardString} to execute on leave.
     */
    public List<RewardString> onLeave = new ArrayList<>();

    public PreviousData(@NotNull Player player) {
        gamemode = player.getGameMode();
        location = player.getLocation();
        hunger = player.getFoodLevel();
        allowFlight = player.getAllowFlight();
        flying = player.getAllowFlight();
        effects = player.getActivePotionEffects();

        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }

        if (Option.INVENTORY_HANDLING) {
            inventoryData = new InventoryData(player);
            inventoryData.save(Option.INVENTORY_SAVING);
        }

        // health handling after removing effects and inventory to avoid them affecting it
        if (Option.HEALTH_HANDLING) {
            health = player.getHealth();
            maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            player.setHealth(maxHealth);
        }
    }

    public void apply(Player player, boolean teleportBack) {
        try {
            if (teleportBack) {
                player.teleport(Option.GO_BACK ? Option.GO_BACK_LOC : location);
            }

            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);
            player.setAllowFlight(allowFlight);
            player.setFlying(flying);

            if (maxHealth != null) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            }
            if (health != null) {
                player.setHealth(health);
            }

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
        } catch (Exception ex) { // not the best way to do this... too bad!
            IP.logging().stack("Error while recovering stats of %s".formatted(player.getName()), ex);
        }

        if (inventoryData != null) {
            inventoryData.apply();
        }
    }
}