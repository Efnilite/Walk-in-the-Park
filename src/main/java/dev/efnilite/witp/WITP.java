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
import dev.efnilite.witp.generator.base.GeneratorOption;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.MultiverseHook;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.hook.ProtocolHook;
import dev.efnilite.witp.internal.gamemode.DefaultGamemode;
import dev.efnilite.witp.internal.gamemode.SpectatorGamemode;
import dev.efnilite.witp.internal.style.DefaultStyleType;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.UpdateChecker;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.sql.Database;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Main class
 *
 * @author Efnilite
 * Copyright (c) 2020-2022
 */
public final class WITP extends FyPlugin {

    public static boolean OUTDATED = false;
    private static Gson gson;
    private static WITP instance;
    private static Database database;
    private static Registry registry;
    private static Configuration configuration;
    private static SubareaDivider divider;

    @Nullable
    private static ProtocolHook protocolHook;

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
        registerCommand("witp", new ParkourCommand());
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
        if (getServer().getPluginManager().isPluginEnabled("ProtocolAPI")) {
            Logging.info("Connecting with ProtocolAPI..");
            protocolHook = new ProtocolHook();
        }
        if (Option.BUNGEECORD.get()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

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
                database = new Database();
                database.connect(Option.SQL_URL.get(), Option.SQL_PORT.get(), Option.SQL_DB.get(),
                        Option.SQL_USERNAME.get(), Option.SQL_PASSWORD.get());
            }
            ParkourUser.initHighScores();
        } catch (Throwable throwable) {
            Logging.stack("There was an error while starting WITP",
                    "Please report this error and the above stack trace to the developer!", throwable);
        }

        // ----- Events -----

        registerListener(new Handler());
        new InventoryBuilder.ClickHandler(this);

        // ----- Update checker -----

        if (Option.UPDATER.get()) {
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
            try {
                ParkourUser.unregister(user, true, false, false);
            } catch (IOException | InvalidStatementException ex) {
                Logging.stack("Error while unregistering player " + user.getPlayer().getName(),
                        "Please report this error to the developer!", ex);
            }
        }

        if (database != null) {
            database.close();
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

    public static @Nullable MultiverseHook getMultiverseHook() {
        return multiverseHook;
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

    public static Gson getGson() {
        return gson;
    }

    public static WITP getInstance() {
        return instance;
    }
}
