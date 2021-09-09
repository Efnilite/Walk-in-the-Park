package dev.efnilite.witp.events;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

public class Handler implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = WITP.getDivider().getWorld();
        if (player.isOp() && WITP.OUTDATED) {
            player.sendMessage(Util.color("&c&l(!) &7The WITP plugin you are using is outdated. " +
                    "Updates usually fix a variety of bugs. Check the Spigot page for more info."));
        }
        if (player.isOp() && WITP.getMultiverseHook() != null && Util.getVoidGenerator() == null) {
            player.sendMessage(Util.color("&c&l(!) &7You're running Multiverse without support for creating void worlds." +
                    "Go to the wiki to add support for this."));
        }
        if (Option.BUNGEECORD) {
            try {
                ParkourPlayer.register(player);
            } catch (IOException | SQLException ex) {
                ex.printStackTrace();
                Verbose.error("Something went wrong while trying to fetch a player's (" + player.getName() + ") data");
            }
            if (Option.JOIN_LEAVE) {
                event.setJoinMessage(null);
                for (ParkourUser user : ParkourUser.getUsers()) {
                    user.sendTranslated("join", player.getName());
                }
            }
        } else if (player.getWorld() == WITP.getDivider().getWorld()) {
            World fallback = Bukkit.getWorld(WITP.getConfiguration().getString("config", "world.fall-back"));
            if (fallback != null) {
                // If players who left in the world end up in the world itself while not being a player
                player.teleport(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                Verbose.error("There is no backup world! Selecting one at random...");
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(world.getName()))) {
                        player.sendMessage(Util.color("&cThere was an error while trying to get a world"));
                        player.teleport(last.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        return;
                    }
                }
                Verbose.error("There are no worlds for player " + player.getName() + " to fall back to! Kicking player..");
                player.kickPlayer("There are no accessible worlds for you to go to - please rejoin");
            }
        }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        ParkourPlayer player = ParkourPlayer.getPlayer(event.getPlayer());
        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;
        if (player != null && action && Duration.between(player.joinTime, Instant.now()).getSeconds() > 1) {
            event.setCancelled(true);
            ItemStack mat = WITP.getConfiguration().getFromItemData(player.locale, "general.menu");
            if (mat == null) {
                Verbose.error("Material for options in config is null - defaulting to compass");
                mat = new ItemBuilder(Material.COMPASS, "&c&lOptions").build();
            }
            if (Util.getHeldItem(player.getPlayer()).getType() == mat.getType()) {
                player.menu();
            }
        }
    }

    @EventHandler
    public void onSwitch(PlayerChangedWorldEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());
        if (event.getFrom().getUID() == WITP.getDivider().getWorld().getUID() && user != null && user.getPlayer().getTicksLived() > 100) {
            try {
                ParkourUser.unregister(user, true, false, true);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to unregister player");
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser player = ParkourUser.getUser(event.getPlayer());
        if (player != null) {
            if (WITP.getConfiguration().getFile("lang").getBoolean("messages.join-leave-enabled")) {
                event.setQuitMessage(null);
                for (ParkourUser user : ParkourUser.getUsers()) {
                    user.sendTranslated("leave", player.getPlayer().getName());
                }
            }
            try {
                ParkourPlayer.unregister(player, true, false, true);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("There was an error while trying to handle player " + player.getPlayer().getName() + " quitting!");
            }
        }
    }

}
