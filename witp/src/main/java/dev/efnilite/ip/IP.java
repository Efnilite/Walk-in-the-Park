package dev.efnilite.ip;

import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.events.Handler;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.MultiverseHook;
import dev.efnilite.ip.hook.PlaceholderHook;
import dev.efnilite.ip.internal.gamemode.DefaultGamemode;
import dev.efnilite.ip.internal.gamemode.SpectatorGamemode;
import dev.efnilite.ip.internal.style.DefaultStyleType;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.reward.RewardReader;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.sql.SQLManager;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.ip.world.WorldHandler;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.elevator.GitElevator;
import dev.efnilite.vilib.util.elevator.VersionComparator;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Main class of Infinite Parkour
 *
 * @author Efnilite
 * Copyright (c) 2020-2022
 */
public final class IP extends ViPlugin {

    public static final String NAME = "<gradient:#B30000>Infinite Parkour</gradient:#00A1A1>";
    public static final String PREFIX = NAME + " <#7B7B7B>» <gray>";
    public static final String REQUIRED_VILIB_VERSION = "1.0.9";

    private static IP instance;
    private static GitElevator elevator;
    private static SQLManager sqlManager;
    private static Registry registry;
    private static WorldDivider divider;
    private static WorldHandler worldHandler;
    private static Configuration configuration;

    @Nullable
    private static MultiverseHook multiverseHook;

    @Nullable
    private static PlaceholderHook placeholderHook;

    @Override
    public void enable() {
        instance = this;

        // ----- Check vilib -----

        Plugin vilib = getServer().getPluginManager().getPlugin("vilib");
        if (vilib == null || !vilib.isEnabled()) {
            getLogger().severe("##");
            getLogger().severe("## Infinite Parkour requires vilib to work!");
            getLogger().severe("##");
            getLogger().severe("## Please download it here:");
            getLogger().severe("## https://github.com/Efnilite/vilib/releases/latest");
            getLogger().severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!VersionComparator.FROM_SEMANTIC.isLatest(REQUIRED_VILIB_VERSION, vilib.getDescription().getVersion())) {
            getLogger().severe("##");
            getLogger().severe("## Infinite Parkour requires *a newer version* of vilib to work!");
            getLogger().severe("##");
            getLogger().severe("## Please download it here: ");
            getLogger().severe("## https://github.com/Efnilite/vilib/releases/latest");
            getLogger().severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ----- Start time -----

        Time.timerStart("load");

        // ----- Configurations -----

        configuration = new Configuration(this);

        Option.init(true);

        divider = new WorldDivider();

        // ----- SQL and data -----

        if (Option.SQL) {
            try {
                sqlManager = new SQLManager();
                sqlManager.connect();
            } catch(Throwable throwable){
                logging().stack("There was an error while starting WITP", throwable);
            }
        }

        // ----- Hooks and Bungee -----

        if (getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            logging().info("Connecting with Multiverse..");
            multiverseHook = new MultiverseHook();
        }

        if (Option.BUNGEECORD.get()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // ----- Worlds -----

        worldHandler = new WorldHandler();
        worldHandler.createWorld();

        // ----- Registry -----

        registry = new Registry();

        registry.register(new DefaultGamemode());
        registry.register(new SpectatorGamemode());
        registry.registerType(new DefaultStyleType());

        registry.getStyleType("default").addConfigStyles("styles.list", configuration.getFile("config"));

        Gamemodes.init();

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging().info("Connecting with Holographic Displays..");
            HoloHook.init(this);
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging().info("Connecting with PlaceholderAPI..");
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }

        // ----- Events -----

        registerListener(new Handler());
        registerCommand("witp", new ParkourCommand());

        // ----- Elevator -----

        elevator = new GitElevator("Efnilite/Walk-in-the-Park", this, VersionComparator.FROM_SEMANTIC, Option.AUTO_UPDATER);

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(RewardReader.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Option.LANGUAGES.get().size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.JOIN_COUNT;
            ParkourUser.JOIN_COUNT = 0;
            return joins;
        }));

        logging().info("Loaded WITP in " + Time.timerEnd("load") + "ms!");
    }

    @Override
    public void disable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            ParkourUser.unregister(user, true, false, false);
        }

        // write all gamemodes
        for (Gamemode gamemode : IP.getRegistry().getGamemodes()) {
            Leaderboard leaderboard = gamemode.getLeaderboard();

            if (leaderboard == null) {
                continue;
            }

            leaderboard.write(false);
        }

        if (sqlManager != null) {
            sqlManager.close();
        }

        if (divider != null) { // somehow this can be null despite it only ever being set to a new instance?
            World world = worldHandler.getWorld();
            if (world != null) {
                for (Player player : world.getPlayers()) {
                    player.kickPlayer("Server is restarting");
                }
            }
        } else {
            World world = Bukkit.getWorld(Option.WORLD_NAME.get());
            if (world != null) {
                for (Player player : world.getPlayers()) {
                    player.kickPlayer("Server is restarting");
                }
            }
        }

        worldHandler.deleteWorld();
    }

    /**
     * Returns the {@link Logging} belonging to this plugin.
     *
     * @return this plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return getPlugin().logging;
    }

    /**
     * Returns this plugin instance.
     *
     * @return the plugin instance.
     */
    public static IP getPlugin() {
        return instance;
    }

    // Static stuff
    @Nullable
    public static MultiverseHook getMultiverseHook() {
        return multiverseHook;
    }

    @Nullable
    public static PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static WorldHandler getWorldHandler() {
        return worldHandler;
    }

    public static Registry getRegistry() {
        return registry;
    }

    public static SQLManager getSqlManager() {
        return sqlManager;
    }

    public static WorldDivider getDivider() {
        return divider;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static GitElevator getElevator() {
        return elevator;
    }
}
