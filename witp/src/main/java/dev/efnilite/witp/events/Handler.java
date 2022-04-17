package dev.efnilite.witp.events;

import dev.efnilite.vilib.chat.Message;
import dev.efnilite.vilib.event.EventWatcher;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.witp.ParkourCommand;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.PersistentUtil;
import dev.efnilite.witp.world.generation.VoidGenerator;
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
        if (player.isOp() && WITP.OUTDATED) {
            Message.send(player, "");
            Message.send(player, WITP.PREFIX + "Your Walk in the Park build is outdated. Updates usually fix crucial bugs. Please update.");
            Message.send(player, "");
        }
        if (player.isOp() && WITP.getMultiverseHook() != null && VoidGenerator.getMultiverseGenerator() == null) {
            Message.send(player, "");
            Message.send(player, WITP.PREFIX + "You are running Multiverse without VoidGen.");
            Message.send(player, WITP.PREFIX + "This causes extreme lag spikes and performance issues while playing.");
            Message.send(player, WITP.PREFIX + "Please visit the wiki to fix this.");
            Message.send(player, "");
        }

        // Bungeecord joining
        if (Option.BUNGEECORD.get()) {
            if (!Option.ENABLE_JOINING.get()) {
                Logging.info("Player " + player.getName() + "tried joining, but parkour is disabled.");
                return;
            }

            if (Option.JOIN_LEAVE_MESSAGES.get()) {
                event.setJoinMessage(null);
            }

            ParkourPlayer.join(player);
        } else if (player.getWorld().getUID().equals(WITP.getWorldHandler().getWorld().getUID())) {
            World fallback = Bukkit.getWorld(WITP.getConfiguration().getString("config", "world.fall-back"));
            if (fallback != null) {
                // If players who left in the world end up in the world itself while not being a player
                player.teleport(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                Logging.error("There is no backup world! Selecting one at random...");
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(Option.WORLD_NAME.get()))) {
                        Message.send(player, WITP.PREFIX + "<red>There was an error while trying to find the parkour world.");
                        player.teleport(last.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        return;
                    }
                }
                Logging.error("There are no worlds for player " + player.getName() + " to fall back to! Kicking player..");
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

        if (Option.JOIN_LEAVE_MESSAGES.get()) {
            event.setQuitMessage(null);
        }

        if (Option.INVENTORY_HANDLING.get()) {
            PreviousData data = user.getPreviousData();
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
        if (Option.FOCUS_MODE.get()) {
            ParkourUser user = ParkourUser.getUser(event.getPlayer());
            if (user != null) {
                String command = event.getMessage().toLowerCase();
                for (String item : Option.FOCUS_MODE_WHITELIST.get()) {   // i.e.: "msg", "w"
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
            case LEFT_CLICK_BLOCK:
                event.setCancelled(true);
                if (ParkourCommand.selections.get(player) == null) {
                    ParkourCommand.selections.put(player, new Selection(location, null, player.getWorld()));
                } else {
                    Location pos2 = ParkourCommand.selections.get(player).getPos2();
                    if (pos2 == null) {
                        Message.send(player, "&4&l(!) Error &7Position two wasn't set. Please retry!");
                        return;
                    }
                    ParkourCommand.selections.put(player, new Selection(location, pos2, player.getWorld()));
                    Particles.box(BoundingBox.of(location, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                Message.send(player, "&4&l(!) &7Position 1 was set to " + Util.toString(location, true));
                break;

            case RIGHT_CLICK_BLOCK:
                event.setCancelled(true);
                if (ParkourCommand.selections.get(player) == null) {
                    ParkourCommand.selections.put(player, new Selection(null, location, player.getWorld()));
                } else {
                    Location pos1 = ParkourCommand.selections.get(player).getPos1();
                    if (pos1 == null) {
                        Message.send(player, "&4&l(!) Error &7Position one wasn't set. Please retry!");
                        return;
                    }
                    ParkourCommand.selections.put(player, new Selection(pos1, location, player.getWorld()));
                    Particles.box(BoundingBox.of(pos1, location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                Message.send(player, "&4&l(!) &7Position 2 was set to " + Util.toString(location, true));
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        ParkourPlayer player = ParkourPlayer.getPlayer(event.getPlayer());
        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;
        if (player != null && action && System.currentTimeMillis() - player.getJoinTime() > 1000) {
            Material menu = WITP.getConfiguration().getFromItemData(player.getLocale(), "general.menu").build().getType();
            Material quit = WITP.getConfiguration().getFromItemData(player.getLocale(), "general.quit").build().getType();
            Material held = Util.getHeldItem(player.getPlayer()).getType();
            if (held == menu) {
                event.setCancelled(true);
                player.getGenerator().menu();
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
        UUID parkourWorld = WITP.getWorldHandler().getWorld().getUID();

        // joining world will kick player if they aren't registered to prevent teleporting to players, exception for players with op
        if (player.getWorld().getUID() == parkourWorld && user == null && !player.isOp()) {
            player.kickPlayer("");
        } // todo test

        // leaving world will unregister player
        if (event.getFrom().getUID() == parkourWorld && user != null && player.getTicksLived() > 100) {
            ParkourUser.unregister(user, false, false, true);
        }
    }
}