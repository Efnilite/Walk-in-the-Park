package dev.efnilite.witp.events;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.inventory.PersistentUtil;
import dev.efnilite.witp.util.particle.ParticleData;
import dev.efnilite.witp.util.particle.Particles;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import org.bukkit.*;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

public class Handler implements Listener {

    /**
     * If a player quits and rejoins, give them their stuff back
     */
    private final HashMap<String, PreviousData> quitInventoryData = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        World world = WITP.getDivider().getWorld();

        PreviousData data = quitInventoryData.get(playerName);
        if (data != null) {
            data.apply();
            quitInventoryData.remove(playerName);
        }

        // OP join messages
        if (player.isOp() && WITP.OUTDATED) {
            player.sendMessage(Util.color("&c&l(!) &7The WITP plugin you are using is outdated. " +
                    "Updates usually fix a variety of bugs. Check the Spigot page for more info."));
        }
        if (player.isOp() && WITP.getMultiverseHook() != null && Util.getVoidGenerator() == null) {
            player.sendMessage(Util.color("&c&l(!) &7You're running Multiverse without support for creating void worlds." +
                    "Go to the wiki to add support for this."));
        }

        // Bungeecord joining
        if (Option.BUNGEECORD) {
            try {
                ParkourPlayer pp = ParkourPlayer.register(player);
                ParkourGenerator generator = new DefaultGenerator(pp);
                WITP.getDivider().generate(pp, generator);
            } catch (IOException | SQLException ex) {
                ex.printStackTrace();
                Verbose.error("Something went wrong while trying to fetch a player's (" + playerName + ") data");
            }

            // Join message
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
    public void leave(PlayerQuitEvent event) {
        Player bPlayer = event.getPlayer();
        String playerName = bPlayer.getName();
        ParkourUser player = ParkourUser.getUser(bPlayer);
        if (player == null) {
            return;
        }

        if (Option.JOIN_LEAVE) {
            event.setQuitMessage(null);
            for (ParkourUser user : ParkourUser.getUsers()) {
                user.sendTranslated("leave", playerName);
            }
        }
        if (Option.INVENTORY_HANDLING) {
            PreviousData data = ParkourUser.getPreviousData(playerName);
            if (data != null)  {
                quitInventoryData.put(playerName, data);
            }
        }
        try {
            ParkourPlayer.unregister(player, true, false, true);
        } catch (IOException | InvalidStatementException ex) {
            ex.printStackTrace();
            Verbose.error("There was an error while trying to handle player " + playerName + " quitting!");
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
                if (MainCommand.selections.get(player) == null) {
                    MainCommand.selections.put(player, new Selection(location, null, player.getWorld()));
                } else {
                    Location pos2 = MainCommand.selections.get(player).getPos2();
                    if (pos2 == null) {
                        MainCommand.send(player, "&4&l(!) Error &7Position two wasn't set. Please retry!");
                        return;
                    }
                    MainCommand.selections.put(player, new Selection(location, pos2, player.getWorld()));
                    Particles.box(BoundingBox.of(location, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                MainCommand.send(player, "&4&l(!) &7Position 1 was set to " + Util.toString(location, true));
                break;
            case RIGHT_CLICK_BLOCK:
                event.setCancelled(true);
                if (MainCommand.selections.get(player) == null) {
                    MainCommand.selections.put(player, new Selection(null, location, player.getWorld()));
                } else {
                    Location pos1 = MainCommand.selections.get(player).getPos1();
                    if (pos1 == null) {
                        MainCommand.send(player, "&4&l(!) Error &7Position one wasn't set. Please retry!");
                        return;
                    }
                    MainCommand.selections.put(player, new Selection(pos1, location, player.getWorld()));
                    Particles.box(BoundingBox.of(pos1, location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                }
                MainCommand.send(player, "&4&l(!) &7Position 2 was set to " + Util.toString(location, true));
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        ParkourPlayer player = ParkourPlayer.getPlayer(event.getPlayer());
        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;
        if (player != null && action && Duration.between(player.joinTime, Instant.now()).toMillis() > 100) {
            event.setCancelled(true);
            ItemStack mat = WITP.getConfiguration().getFromItemData(player.locale, "general.menu");
            if (mat == null) {
                Verbose.error("Material for options in config is null - defaulting to compass");
                mat = new ItemBuilder(Material.COMPASS, "&c&lOptions").build();
            }
            if (Util.getHeldItem(player.getPlayer()).getType() == mat.getType()) {
                player.getGenerator().menu();
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
}