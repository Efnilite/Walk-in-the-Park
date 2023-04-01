package dev.efnilite.ip;

import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
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

import java.util.HashMap;
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
    private final HashMap<String, PreviousData> quitPreviousData = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        PreviousData data = quitPreviousData.get(playerName);
        if (data != null) {
            data.apply(false);
            quitPreviousData.remove(playerName);
        }

        // OP join messages
        if (player.isOp() && IP.getPlugin().getElevator().isOutdated()) {
            send(player, "");
            send(player, IP.PREFIX + "Your version is outdated. " + "Please visit the Spigot page to update.");
            send(player, "");
        }
        if (player.isOp() && WorldManagerMV.MANAGER != null && VoidGenerator.getMultiverseGenerator() == null) {
            send(player, "");
            send(player, IP.PREFIX + "You are running Multiverse without VoidGen. " +
                    "This causes extreme lag spikes and performance issues while playing. Please visit the wiki to fix this.");
            send(player, "");
        }

        // Bungeecord joining
        if (Option.BUNGEECORD) {
            Modes.DEFAULT.create(player);
        } else if (player.getWorld().getUID().equals(WorldManager.getWorld().getUID())) {
            World fallback = Bukkit.getWorld(Config.CONFIG.getString("world.fall-back"));
            if (fallback != null) {
                // If players who left in the world end up in the world itself while not being a player
                player.teleport(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                IP.logging().warn("No backup worlds have been set! A random one will be selected for " + playerName);
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(Option.WORLD_NAME))) {
                        send(player, IP.PREFIX + "<red>There was an error while trying to find the parkour world.");
                        player.teleport(last.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        return;
                    }
                }
                IP.logging().error("There are no worlds for player " + playerName + " to fall back to. " + playerName + " will be kicked.");
                player.kickPlayer("There are no accessible worlds for you to go to - please rejoin");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void tpSpec(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        ParkourUser user = ParkourUser.getUser(player);
        if (user == null) { // the player who teleported is null
            return;
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        if (Option.INVENTORY_HANDLING) {
            PreviousData data = user.previousData;
            if (data != null) {
                quitPreviousData.put(user.getName(), data);
            }
        }

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
        for (String item : Option.FOCUS_MODE_WHITELIST) {   // i.e.: "msg", "w"
            if (command.contains(item.toLowerCase())) {     // "/msg Efnilite hi" contains "msg"?
                return;                                     // yes, so let event go through
            }
        }

        user.sendTranslated("other.no_do");
        event.setCancelled(true);
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

    private void handleRestriction(Player player, Cancellable event) {
        if (ParkourUser.getUser(player) == null) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        // prevent users from transferring items to possible vaults
        if (event.getInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("ip.admin") ||
                item.getItemMeta() == null ||
                !item.getItemMeta().getDisplayName().contains("Schematic Wand") ||
                event.getClickedBlock() == null ||
                event.getHand() != EquipmentSlot.HAND) {
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

        if (held == play) {
            event.setCancelled(true);
            Menus.PLAY.open(player);
        } else if (held == community) {
            event.setCancelled(true);
            Menus.COMMUNITY.open(player);
        } else if (held == settings) {
            event.setCancelled(true);
            Menus.SETTINGS.open(player);
        } else if (held == lobby) {
            event.setCancelled(true);
            Menus.LOBBY.open(player);
        } else if (held == quit) {
            event.setCancelled(true);
            ParkourUser.leave(player);
        }
    }

    private ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    @EventHandler
    public void onSwitch(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        UUID parkourWorld = WorldManager.getWorld().getUID();

        boolean passes = Option.PERMISSIONS ? ParkourOption.ADMIN.mayPerform(player) : player.isOp();

        // joining world will kick player if they aren't registered to prevent teleporting to players, exception for players with op
        if (player.getWorld().getUID() == parkourWorld && user == null && !passes) {
            player.kickPlayer("You can't enter the parkour world by teleporting!");
        }

        // leaving world will unregister player
        if (event.getFrom().getUID() == parkourWorld && user != null && player.getTicksLived() > 100) {
            ParkourUser.unregister(user, false, false, true);
        }
    }
}