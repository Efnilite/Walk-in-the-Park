package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.reward.Reward;
import dev.efnilite.ip.world.World;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Colls;
import dev.efnilite.vilib.util.Strings;
import io.papermc.lib.PaperLib;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PreviousData {

    public Map<Mode, Set<Reward>> leaveRewards = new HashMap<>();

    private final Player player;

    private final int foodLevel;
    private final float saturation;
    private final boolean flying;
    private final boolean allowFlight;

    private final GameMode gameMode;
    private final Location position;
    private final ItemStack[] inventoryContents;
    private final Collection<PotionEffect> effects;

    public PreviousData(Player player) {
        this.player = player;

        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.flying = player.isFlying();
        this.allowFlight = player.getAllowFlight();

        this.gameMode = player.getGameMode();
        this.position = player.getLocation();
        this.inventoryContents = player.getInventory().getContents();
        this.effects = player.getActivePotionEffects();
    }

    /**
     * Sets up the player's inventory and properties.
     * @param vector The location to teleport the player to.
     * @return A future that completes when the teleportation is done.
     */
    public CompletableFuture<Boolean> setup(Vector vector) {
        var future = new CompletableFuture<Boolean>();

        PaperLib.teleportAsync(player, vector.toLocation(World.getWorld())).thenRun(() -> {
            IP.log("Setting up inventory for player %s".formatted(player.getName()));

            player.setGameMode(GameMode.ADVENTURE);
            player.setFallDistance(0F);

            player.resetPlayerTime();
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

            player.setFoodLevel(20);
            player.setSaturation(20F);
            player.setFlying(false);
            player.setAllowFlight(false);

            if (Config.CONFIG.getBoolean("options.inventory-handling")) {
                player.sendMessage(Strings.colour(Locales.getString(player, "other.customize")));
            } else {
                player.getInventory().clear();

                List<Item> items = new ArrayList<>();

                if (ParkourOption.PLAY.mayPerform(player)) items.add(Locales.getItem(player, "play.item"));
                if (ParkourOption.COMMUNITY.mayPerform(player)) items.add(Locales.getItem(player, "community.item"));
                if (ParkourOption.SETTINGS.mayPerform(player)) items.add(Locales.getItem(player, "settings.item"));
                if (ParkourOption.LOBBY.mayPerform(player)) items.add(Locales.getItem(player, "lobby.item"));
                if (ParkourOption.QUIT.mayPerform(player)) items.add(Locales.getItem(player, "other.quit"));

                List<Integer> slots = Menu.getEvenlyDistributedSlots(items.size());

                Colls.range(0, items.size()).forEach(idx -> player.getInventory().setItem(slots.get(idx), items.get(idx).build()));
            }

            future.complete(true);
        });

        return future;
    }

    /**
     * Resets the player's data.
     */
    public void reset(boolean switchMode, boolean urgent) {
        if (switchMode) {
            reset();

            return;
        }
        if (urgent) {
            player.teleport(position);
            reset();

            return;
        }

        PaperLib.teleportAsync(player, position).thenRun(this::reset);
    }

    private void reset() {
        player.setFallDistance(0);

        player.setGameMode(gameMode);
        player.getInventory().setContents(inventoryContents);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));
        player.addPotionEffects(effects);

        player.resetPlayerTime();

        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setFlying(flying);
        player.setAllowFlight(allowFlight);

        leaveRewards.forEach((gameMode, rewards) -> rewards.forEach(reward -> reward.execute(player, gameMode)));
    }
}