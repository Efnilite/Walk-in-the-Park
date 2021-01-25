package dev.efnilite.witp;

import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.*;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.sql.Database;
import dev.efnilite.witp.util.task.Tasks;
import dev.efnilite.witp.util.web.Metrics;
import dev.efnilite.witp.util.web.UpdateChecker;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import dev.efnilite.witp.version.VersionManager;
import dev.efnilite.witp.version.VersionManager_v1_16_R2;
import dev.efnilite.witp.version.VersionManager_v1_16_R3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class WITP extends JavaPlugin implements Listener {

    public static boolean isOutdated = false;
    private static WITP instance;
    private static Database database;
    private static Configuration configuration;
    private static VersionManager versionManager;
    private static SubareaDivider divider;
    private static UpdateChecker checker;

    @Override
    public void onEnable() {
        instance = this;
        Verbose.init();

        String version = Util.getVersion();
        switch (version) {
            case "v1_16_R3":
                versionManager = new VersionManager_v1_16_R3();
                break;
            case "v1_16_R2":
                versionManager = new VersionManager_v1_16_R2();
                break;
            default:
                Verbose.error("You are trying to start this plugin using an invalid server version");
                Verbose.error("This plugin only works in version 1.16.4, 1.16.3 or 1.16.2");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
        }

        configuration = new Configuration(this);
        if (configuration.getFile("config").getBoolean("metrics")) {
            new Metrics(this, 9272);
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook().register();
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        Option.init(true);
        this.getServer().getPluginManager().registerEvents(this, this);
        addCommand("witp", new MainCommand());
        divider = new SubareaDivider();
        checker = new UpdateChecker();

        if (Option.SQL) {
//            database = new Database();
//            database.connect(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB, Option.SQL_USERNAME, Option.SQL_PASSWORD);
        }
        ParkourUser.initHighScores();

        new InventoryBuilder.ClickHandler(this);
        Tasks.syncRepeat(new BukkitRunnable() {
            @Override
            public void run() {
                checker.check();
            }
        }, 30 * 60 * 20);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        Bukkit.getScheduler().cancelTasks(this);
        if (database != null) {
            database.close();
        }

        for (ParkourUser user : ParkourUser.getUsers()) {
            try {
                ParkourUser.unregister(user, true, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while unregistering");
            }
        }
    }

    private void addCommand(String name, BukkitCommand wrapper) {
        PluginCommand command = getCommand(name);

        if (command == null) {
            throw new IllegalStateException("Command is null");
        }

        command.setExecutor(wrapper);
        command.setTabCompleter(wrapper);
    }

    @Nullable
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        String config = WITP.getConfiguration().getString("config", "world.name");
        if (config == null) {
            Verbose.error("World name is null");
            config = "witp";
        }
        if (worldName.equalsIgnoreCase(config)) {
            return new VoidGenerator();
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = WITP.getDivider().getWorld();
        if (Option.BUNGEECORD) {
            try {
                ParkourPlayer.register(player);
            } catch (IOException ex) {
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
            World fallback = Bukkit.getWorld(configuration.getString("config", "world.fall-back"));
            if (fallback != null) {
                player.teleport(fallback.getSpawnLocation());
                player.sendMessage("You have been teleported to a backup location");
            } else {
                Verbose.error("There is no backup world! Selecting one at random...");
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(world.getName()))) {
                        player.sendMessage(Util.color("&cThere was an error while trying to get a world"));
                        player.teleport(last.getSpawnLocation());
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

    @EventHandler
    public void command(PlayerCommandPreprocessEvent event) {
        if (Option.FOCUS_MODE) {
            ParkourUser user = ParkourUser.getUser(event.getPlayer());
            if (user != null && !(event.getMessage().toLowerCase().contains("witp"))) {
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
        if (ParkourPlayer.getPlayer(event.getPlayer()) != null &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            String mat = WITP.getConfiguration().getString("config", "options.item");
            if (mat == null) {
                Verbose.error("Material for options in config is null - defaulting to compass");
                mat = "COMPASS";
            }
            if (Util.getHeldItem(player.getPlayer()).getType() == Material.getMaterial(mat.toUpperCase())) {
                player.menu();
            }
        }
    }

    @EventHandler
    public void onSwitch(PlayerChangedWorldEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());
        if (event.getFrom().getUID() == WITP.getDivider().getWorld().getUID() && user != null && user.getPlayer().getTicksLived() > 100) {
            try {
                ParkourUser.unregister(user, true, false);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to unregister player");
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourPlayer player = ParkourPlayer.getPlayer(event.getPlayer());
        if (player != null) {
            if (configuration.getFile("lang").getBoolean("messages.join-leave-enabled")) {
                event.setQuitMessage(null);
                for (ParkourUser user : ParkourUser.getUsers()) {
                    user.sendTranslated("leave", player.getPlayer().getName());
                }
            }
            try {
                ParkourPlayer.unregister(player, true, false);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("There was an error while trying to handle player " + player.getPlayer().getName() + " quitting!s");
            }
        }
    }

    public static Database getDatabase() {
        return database;
    }

    public static SubareaDivider getDivider() {
        return divider;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static VersionManager getVersionManager() {
        return versionManager;
    }

    public static WITP getInstance() {
        return instance;
    }
}
