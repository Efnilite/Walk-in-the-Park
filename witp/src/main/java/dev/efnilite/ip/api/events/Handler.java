package dev.efnilite.ip.api.events;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourCommand;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.inventory.PersistentUtil;
import dev.efnilite.ip.world.generation.VoidGenerator;
import dev.efnilite.vilib.event.EventWatcher;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.UUID;

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
            Util.send(player, "");
            Util.send(player,
                    IP.PREFIX + "Your version is outdated. " +
                            "Please visit the Spigot page to update.");
            Util.send(player, "");
        }
        if (player.isOp() && IP.getMultiverseHook() != null && VoidGenerator.getMultiverseGenerator() == null) {
            Util.send(player, "");
            Util.send(player, IP.PREFIX + "You are running Multiverse without VoidGen.");
            Util.send(player, IP.PREFIX + "This causes extreme lag spikes and performance issues while playing.");
            Util.send(player, IP.PREFIX + "Please visit the wiki to fix this.");
            Util.send(player, "");
        }

        // Bungeecord joining
        if (Option.BUNGEECORD) {
            if (!Option.ENABLE_JOINING) {
                IP.logging().warn("Player " + player.getName() + " tried joining, but parkour is disabled.");
                return;
            }

            if (Option.JOIN_LEAVE_MESSAGES) {
                event.setJoinMessage(null);
            }

            ParkourPlayer.joinDefault(player);
        } else if (player.getWorld().getUID().equals(IP.getWorldHandler().getWorld().getUID())) {
            World fallback = Bukkit.getWorld(IP.getConfiguration().getString("config", "world.fall-back"));
            if (fallback != null) {
                // If players who left in the world end up in the world itself while not being a player
                player.teleport(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                IP.logging().warn("No backup worlds have been set! A random one will be selected for " + playerName);
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(Option.WORLD_NAME))) {
                        Util.send(player, IP.PREFIX + "<red>There was an error while trying to find the parkour world.");
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
        Player player = event.getPlayer();
        String playerName = player.getName();
        ParkourUser user = ParkourUser.getUser(player);
        if (user == null) {
            return;
        }

        if (Option.JOIN_LEAVE_MESSAGES) {
            event.setQuitMessage(null);
        }

        if (Option.INVENTORY_HANDLING) {
            PreviousData data = user.previousData;
            if (data != null)  {
                quitPreviousData.put(playerName, data);
            }
        }

        ParkourPlayer.leave(user);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (ParkourUser.getUser((Player) event.getEntity()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void command(PlayerCommandPreprocessEvent event) {
        if (Option.FOCUS_MODE) {
            ParkourUser user = ParkourUser.getUser(event.getPlayer());
            if (user != null) {
                String command = event.getMessage().toLowerCase();
                for (String item : Option.FOCUS_MODE_WHITELIST) {   // i.e.: "msg", "w"
                    if (command.contains(item.toLowerCase())) {     // "/msg Efnilite hi" contains "msg"?
                        return;                                     // yes, so let event go through
                    }
                }
                event.setCancelled(true);
                user.sendTranslated("cant-do");
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (ParkourUser.getUser(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (ParkourUser.getUser(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (ParkourUser.getUser(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("witp.schematic") || item.getType().isAir() || !PersistentUtil.hasPersistentData(item, "witp", PersistentDataType.STRING) || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                event.setCancelled(true);
                if (ParkourCommand.selections.get(player) == null) {
                    ParkourCommand.selections.put(player, new Selection(location, null, player.getWorld()));
                } else {
                    Location pos2 = ParkourCommand.selections.get(player).getPos2();
                    if (pos2 == null) {
                        Util.send(player, "&4&l(!) Error &7Position two wasn't set. Please retry!");
                        return;
                    }
                    ParkourCommand.selections.put(player, new Selection(location, pos2, player.getWorld()));
                    Particles.box(BoundingBox.of(location, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                Util.send(player, "&4&l(!) &7Position 1 was set to " + Locations.toString(location, true));
            }
            case RIGHT_CLICK_BLOCK -> {
                event.setCancelled(true);
                if (ParkourCommand.selections.get(player) == null) {
                    ParkourCommand.selections.put(player, new Selection(null, location, player.getWorld()));
                } else {
                    Location pos1 = ParkourCommand.selections.get(player).getPos1();
                    if (pos1 == null) {
                        Util.send(player, "&4&l(!) Error &7Position one wasn't set. Please retry!");
                        return;
                    }
                    ParkourCommand.selections.put(player, new Selection(pos1, location, player.getWorld()));
                    Particles.box(BoundingBox.of(pos1, location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                Util.send(player, "&4&l(!) &7Position 2 was set to " + Locations.toString(location, true));
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

        boolean type = event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.DISPENSER ||
                event.getClickedBlock().getType() == Material.DROPPER ||
                event.getClickedBlock().getType() == Material.HOPPER);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && type && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;

        if (action && System.currentTimeMillis() - pp.getJoinTime() > 1000) {
            Material held = Util.getHeldItem(player).getType();

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
    }

    @EventHandler
    public void onSwitch(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        UUID parkourWorld = IP.getWorldHandler().getWorld().getUID();

        // joining world will kick player if they aren't registered to prevent teleporting to players, exception for players with op
        if (player.getWorld().getUID() == parkourWorld && user == null && !player.isOp()) {
            player.kickPlayer("");
        }

        // leaving world will unregister player
        if (event.getFrom().getUID() == parkourWorld && user != null && player.getTicksLived() > 100) {
            ParkourUser.unregister(user, false, false, true);
        }
    }
}