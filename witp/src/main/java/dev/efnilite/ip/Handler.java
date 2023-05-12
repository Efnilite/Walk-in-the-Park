package dev.efnilite.ip;

import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.world.VoidGenerator;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.ip.world.WorldManagerMV;
import dev.efnilite.vilib.event.EventWatcher;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.UUID;

import static dev.efnilite.ip.util.Util.send;

/**
 * Internal event handler
 */
@ApiStatus.Internal
public class Handler implements EventWatcher {

    /**
     * If a player quits and rejoins, give them their stuff back
     */
    private final HashMap<UUID, PreviousData> quitPreviousData = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // admin messages
        if (player.isOp() && IP.getPlugin().getElevator().isOutdated()) {
            send(player, "");
            send(player, IP.PREFIX + "Your version is outdated. Please visit the Spigot page to update.");
            send(player, "");
        }

        if (player.isOp() && WorldManagerMV.MANAGER != null && VoidGenerator.getMultiverseGenerator() == null) {
            send(player, "");
            send(player, IP.PREFIX + "You are running Multiverse without VoidGen. This causes extreme lag spikes and performance issues while playing. Please visit the wiki to fix this.");
            send(player, "");
        }

        if (quitPreviousData.containsKey(uuid)) {
            quitPreviousData.get(uuid).apply(player, false);
            quitPreviousData.remove(uuid);
        }

        if (Option.ON_JOIN) {
            Modes.DEFAULT.create(player);
            return;
        }

        if (!player.getWorld().equals(WorldManager.getWorld())) {
            return;
        }

        World fallback = Bukkit.getWorld(Config.CONFIG.getString("world.fall-back"));

        if (fallback != null) {
            player.teleport(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return;
        }

        player.teleport(Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(WorldManager.getWorld()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No fallback world was found!"))
                .getSpawnLocation(),
            PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        quitPreviousData.put(user.getUUID(), user.previousData);

        ParkourUser.leave(user);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!Option.FOCUS_MODE) {
            return;
        }

        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        if (Option.FOCUS_MODE_WHITELIST.stream().anyMatch(c -> command.contains(c.toLowerCase()))) {
            return;
        }

        user.sendTranslated("other.no_do");
        event.setCancelled(true);
    }


    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("ip.admin") || item.getItemMeta() == null || !item.getItemMeta().getDisplayName().contains("Schematic Wand") || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        Location[] existingSelection = ParkourCommand.selections.get(player);

        event.setCancelled(true);

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                send(player, IP.PREFIX + "Position 1 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    ParkourCommand.selections.put(player, new Location[]{location, null});
                    return;
                }

                ParkourCommand.selections.put(player, new Location[]{location, existingSelection[1]});

                Particles.box(BoundingBox.of(location, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
            case RIGHT_CLICK_BLOCK -> {
                send(player, IP.PREFIX + "Position 2 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    ParkourCommand.selections.put(player, new Location[]{null, location});
                    return;
                }

                ParkourCommand.selections.put(player, new Location[]{existingSelection[0], location});

                Particles.box(BoundingBox.of(existingSelection[0], location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (pp == null) {
            return;
        }

        boolean type = event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.DISPENSER || event.getClickedBlock().getType() == Material.DROPPER || event.getClickedBlock().getType() == Material.HOPPER);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && type && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;

        if (!action) {
            return;
        }

        Material held = getHeldItem(player).getType();

        Material play = Locales.getItem(player, "play.item").getMaterial();
        Material community = Locales.getItem(player, "community.item").getMaterial();
        Material settings = Locales.getItem(player, "settings.item").getMaterial();
        Material lobby = Locales.getItem(player, "lobby.item").getMaterial();
        Material quit = Locales.getItem(player, "other.quit").getMaterial();

        event.setCancelled(true);

        if (held == play) {
            Menus.PLAY.open(player);
        } else if (held == community) {
            Menus.COMMUNITY.open(player);
        } else if (held == settings) {
            Menus.SETTINGS.open(player);
        } else if (held == lobby) {
            Menus.LOBBY.open(player);
        } else if (held == quit) {
            ParkourUser.leave(player);
        } else {
            event.setCancelled(false);
        }
    }

    private ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    @EventHandler
    public void switchWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        World parkour = WorldManager.getWorld();

        boolean isAdmin = Option.PERMISSIONS ? ParkourOption.ADMIN.mayPerform(player) : player.isOp();

        if (player.getWorld() == parkour && user == null && !isAdmin && player.getTicksLived() > 20) {
            player.kickPlayer("You can't enter the parkour world by teleporting!");
            return;
        }

        if (event.getFrom() == parkour && user != null && Duration.between(user.joined, Instant.now()).toMillis() > 100) {
            ParkourUser.unregister(user, false, false);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler
    public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getInventory().getType() == InventoryType.CRAFTING) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spectate(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }

        handleRestriction(event.getPlayer(), event);
    }

    private void handleRestriction(Player player, Cancellable event) {
        if (!ParkourUser.isUser(player)) {
            return;
        }

        event.setCancelled(true);
    }
}