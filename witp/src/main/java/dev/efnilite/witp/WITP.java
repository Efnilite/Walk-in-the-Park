package dev.efnilite.witp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.efnilite.fycore.FyPlugin;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Task;
import dev.efnilite.fycore.util.Time;
import dev.efnilite.fycore.util.Version;
import dev.efnilite.witp.api.Registry;
import dev.efnilite.witp.events.Handler;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.WorldHandler;
import dev.efnilite.witp.generator.base.GeneratorOption;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.MultiverseHook;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.internal.gamemode.DefaultGamemode;
import dev.efnilite.witp.internal.gamemode.SpectatorGamemode;
import dev.efnilite.witp.internal.style.DefaultStyleType;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.UpdateChecker;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.SQLManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Main class
 *
 * @author Efnilite
 * Copyright (c) 2020-2022
 */
public final class WITP extends FyPlugin {

    public static final String NAME = "<gradient:#B30000>Infinite Parkour</gradient:#00A1A1>";
    public static final String PREFIX = NAME + " <#7B7B7B>» <gray>";

    public static boolean OUTDATED = false;
    private static Gson gson;
    private static WITP instance;
    private static SQLManager sqlManager;
    private static Registry registry;
    private static SubareaDivider divider;
    private static WorldHandler worldHandler;
    private static Configuration configuration;

    @Nullable
    private static MultiverseHook multiverseHook;

    @Nullable
    private static PlaceholderHook placeholderHook;

    @Override
    public void enable() {
        // ----- Instance and timing -----

        instance = this;
        Time.timerStart("load");
        Logging.init(this);
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setLenient().create();

        // ----- Configurations -----

        configuration = new Configuration(this);
        Option.init(true);

        divider = new SubareaDivider();

        // ----- Hooks and Bungee -----

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Logging.info("Connecting with PlaceholderAPI..");
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }
        if (getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Logging.info("Connecting with Multiverse..");
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

        for (String style : Option.STYLES.get()) {
            registry.getStyleType("default").addConfigStyle(style);
        }

        // ----- SQL and data -----

        try {
            if (Option.SQL.get()) {
                sqlManager = new SQLManager();
                sqlManager.connect();
            }
            ParkourUser.initHighScores();
        } catch (Throwable throwable) {
            Logging.stack("There was an error while starting WITP",
                    "Please report this error and the above stack trace to the developer!", throwable);
        }

        // ----- Events -----

        registerListener(new Handler());
        registerCommand("witp", new ParkourCommand());

        // ----- Update checker -----

        if (Option.UPDATE_CHECKER.get()) {
            UpdateChecker checker = new UpdateChecker();

            new Task()
                    .repeat(8 * 72000) // 8 hours
                    .execute(checker::check)
                    .run();
        }

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL.get())));
        metrics.addCustomChart(new SimplePie("using_logs", () -> Boolean.toString(Option.GAMELOGS.get())));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Option.LANGUAGES.get().size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.JOIN_COUNT;
            ParkourUser.JOIN_COUNT = 0;
            return joins;
        }));

        Logging.info("Loaded WITP in " + Time.timerEnd("load") + "ms!");

    }

    @Override
    public void disable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            ParkourUser.unregister(user, true, false, false);
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
     * Gets a DefaultGenerator which disables schematics if the version is below 1.16.
     *
     * @param   player
     *          The player
     *
     * @return a {@link DefaultGenerator}
     */
    public static DefaultGenerator getVersionGenerator(ParkourPlayer player) {
        if (versionSupportsSchematics()) {
            return new DefaultGenerator(player);
        } else {
            return new DefaultGenerator(player, GeneratorOption.DISABLE_SCHEMATICS);
        }
    }

    /**
     * Checks whether the current version supports schematics
     *
     * @return true if it supports it, false if not.
     */
    public static boolean versionSupportsSchematics() {
        return Version.isHigherOrEqual(Version.V1_16);
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

    public static SubareaDivider getDivider() {
        return divider;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static Gson getGson() {
        return gson;
    }

    public static WITP getInstance() {
        return instance;
    }
}
