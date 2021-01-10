package dev.efnilite.witp;

import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Configuration;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.VoidGenerator;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class WITP extends JavaPlugin implements Listener {

    public static boolean isOutdated = false;
    private static WITP instance;
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

        ParkourGenerator.Configurable.init();
        this.getServer().getPluginManager().registerEvents(this, this);
        addCommand("witp", new MainCommand());
        divider = new SubareaDivider();

        checker = new UpdateChecker();

        ParkourUser.initHighScores();
        Tasks.syncRepeat(new BukkitRunnable() {
            @Override
            public void run() {
                checker.check();
            }
        }, 30 * 60 * 20);
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
        if (configuration.getFile("config").getBoolean("bungeecord.enabled")) {
            try {
                ParkourPlayer.register(player);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Something went wrong while trying to fetch a player's (" + player.getName() + ") data");
            }
            if (configuration.getFile("lang").getBoolean("messages.join-leave-enabled")) {
                event.setJoinMessage(null);
                for (ParkourUser user : ParkourUser.getUsers()) {
                    user.sendTranslated("join", player.getName());
                }
            }
        } else if (player.getWorld() == WITP.getDivider().getWorld()) {
            World fallback = Bukkit.getWorld(configuration.getString("config", "world.fall-back"));
            if (fallback != null) {
                player.teleport(fallback.getSpawnLocation());
            } else {
                Verbose.error("There is no backup world! Selecting one at random...");
                for (World last : Bukkit.getWorlds()) {
                    if (!(last.getName().equals(world.getName()))) {
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
        if (ParkourGenerator.Configurable.FOCUS_MODE) {
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
        if (event.getFrom().getUID() == WITP.getDivider().getWorld().getUID() && user != null) {
            try {
                ParkourUser.unregister(user, true);
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
                ParkourPlayer.unregister(player, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("There was an error while trying to handle player " + player.getPlayer().getName() + " quitting!s");
            }
        }
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
