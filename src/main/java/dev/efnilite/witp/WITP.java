package dev.efnilite.witp;

import dev.efnilite.witp.api.Registry;
import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.MultiverseHook;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.hook.ProtocolHook;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.sql.Database;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.task.Tasks;
import dev.efnilite.witp.util.web.Metrics;
import dev.efnilite.witp.util.web.UpdateChecker;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import dev.efnilite.witp.version.*;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

public final class WITP extends JavaPlugin implements Listener {

    public static boolean OUTDATED = false;
    private static WITP instance;
    private static Database database;
    private static Configuration configuration;
    private static VersionManager versionManager;
    private static SubareaDivider divider;
    private static Registry registry;
    private static @Nullable MultiverseHook multiverseHook;
    private static @Nullable ProtocolHook protocolHook;
    private static @Nullable PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;
        Tasks.time("load");
        registry = new Registry();
        Verbose.init();

        // Get correct VersionManager
        String version = Util.getVersion();
        switch (version) {
            case "v1_17_R1":
//                versionManager = new VersionManager_v1_17_R1();
                break;
            case "v1_16_R3":
//                versionManager = new VersionManager_v1_16_R3();
                break;
            case "v1_16_R2":
//                versionManager = new VersionManager_v1_16_R2();
                break;
            case "v1_16_R1":
//                versionManager = new VersionManager_v1_16_R1();
                break;
            default:
                Verbose.error("You are trying to start this plugin using an invalid server version");
                Verbose.error("This plugin only works in version 1.17 or 1.16!");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Verbose.info("Connecting with PlaceholderAPI..");
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") != null) {
            Verbose.info("Connecting with Multiverse..");
            multiverseHook = new MultiverseHook();
        }
        if (getServer().getPluginManager().getPlugin("ProtocolAPI") != null) {
            Verbose.info("Connecting with ProtocolAPI..");
            protocolHook = new ProtocolHook();
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Init config
        configuration = new Configuration(this);

        Option.init(true);
        addCommand("witp", new MainCommand());
        divider = new SubareaDivider();

        if (Option.SQL) {
            database = new Database();
            database.connect(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB, Option.SQL_USERNAME, Option.SQL_PASSWORD);
        }
        ParkourUser.initHighScores();

        // Events
        this.getServer().getPluginManager().registerEvents(this, this);
        new InventoryBuilder.ClickHandler(this);

        UpdateChecker checker = new UpdateChecker();
        Tasks.syncRepeat(checker::check, 4 * 72000); // 4 hours

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new Metrics.SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new Metrics.SimplePie("using_logs", () -> Boolean.toString(Option.GAMELOGS)));
        metrics.addCustomChart(new Metrics.SimplePie("locale_count", () -> Integer.toString(Option.LANGUAGES.size())));
        metrics.addCustomChart(new Metrics.SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.JOIN_COUNT;
            ParkourUser.JOIN_COUNT = 0;
            return joins;
        }));
        long time = Tasks.end("load");
        Verbose.info("Loaded WITP in " + time + "ms!");
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
                ParkourUser.unregister(user, true, true, false);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while unregistering");
            }
        }
        if (divider != null) { // somehow this can be null despite it only ever being set to a new instance?
            for (Player player : divider.getWorld().getPlayers()) {
                player.kickPlayer("Server is restarting");
            }
            Bukkit.unloadWorld(divider.getWorld(), false);
        } else {
            String name = configuration.getString("config", "world.name");
            World world = Bukkit.getWorld(name);
            for (Player player : world.getPlayers()) {
                player.kickPlayer("Server is restarting");
            }
            Bukkit.unloadWorld(world, false);
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

    // Events

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = WITP.getDivider().getWorld();
        if (player.isOp() && WITP.OUTDATED) {
            player.sendMessage(Util.color("&c&l(!) &7The WITP plugin you are using is outdated. " +
                    "Updates usually fix a variety of bugs. Check the Spigot page for more info."));
        }
        if (player.isOp() && multiverseHook != null && Util.getVoidGenerator() == null) {
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
            World fallback = Bukkit.getWorld(configuration.getString("config", "world.fall-back"));
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
            if (configuration.getFile("lang").getBoolean("messages.join-leave-enabled")) {
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

    // Static stuff

    public static @Nullable MultiverseHook getMultiverseHook() {
        return multiverseHook;
    }

    public static @Nullable ProtocolHook getProtocolHook() {
        return protocolHook;
    }

    public static @Nullable PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static Registry getRegistry() {
        return registry;
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
