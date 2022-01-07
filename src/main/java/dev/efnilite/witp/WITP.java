package dev.efnilite.witp;

import dev.efnilite.witp.api.Registry;
import dev.efnilite.witp.api.gamemode.DefaultGamemode;
import dev.efnilite.witp.api.gamemode.SpectatorGamemode;
import dev.efnilite.witp.api.style.DefaultStyleType;
import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.events.Handler;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.GeneratorOption;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.hook.*;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.Version;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.sql.Database;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.task.Tasks;
import dev.efnilite.witp.util.web.UpdateChecker;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class WITP extends JavaPlugin {

    public static boolean OUTDATED = false;
    private static WITP instance;
    private static Database database;
    private static Configuration configuration;
    private static SubareaDivider divider;
    private static Registry registry;
    private static @Nullable MultiverseHook multiverseHook;
    private static @Nullable ProtocolHook protocolHook;
    private static @Nullable PlaceholderHook placeholderHook;
    private static @Nullable NoteHook noteHook;
    private static @Nullable HoloHook holoHook;

    @Override
    public void onEnable() {

        // ----- Instance and timing -----

        instance = this;
        Tasks.time("load");
        Verbose.init();

        // ----- Versions -----

        String version = Util.getVersion();
        switch (version.substring(0, 5)) {
            case "v1_18":
                Verbose.info("Registered under server version 1.18");
                Version.VERSION = Version.V1_18;
                break;
            case "v1_17":
                Verbose.info("Registered under server version 1.17");
                Version.VERSION = Version.V1_17;
                break;
            case "v1_16":
                Verbose.info("Registered under server version 1.16");
                Version.VERSION = Version.V1_16;
                break;
            case "v1_15":
                Verbose.info("Registered under server version 1.15");
                Version.VERSION = Version.V1_15;
                break;
            case "v1_14":
                Verbose.info("Registered under server version 1.14");
                Version.VERSION = Version.V1_14;
                break;
            case "v1_13":
                Verbose.info("Registered under server version 1.13");
                Version.VERSION = Version.V1_13;
                break;
            case "v1_12":
                Verbose.info("Registered under server version 1.12");
                Version.VERSION = Version.V1_12;
                break;
            case "v1_11":
                Verbose.info("Registered under server version 1.11");
                Version.VERSION = Version.V1_11;
                break;
            case "v1_10":
                Verbose.info("Registered under server version 1.10");
                Version.VERSION = Version.V1_10;
                break;
            case "v1_9_":
                Verbose.info("Registered under server version 1.9");
                Version.VERSION = Version.V1_9;
                break;
            case "v1_8_":
                Verbose.info("Registered under server version 1.8");
                Version.VERSION = Version.V1_8;
                break;
        }

        // ----- Configurations -----

        configuration = new Configuration(this);
        Option.init(true);
        addCommand("witp", new MainCommand());
        divider = new SubareaDivider();

        // ----- Hooks and Bungee -----

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Verbose.info("Connecting with PlaceholderAPI..");
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }
        if (getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Verbose.info("Connecting with Multiverse..");
            multiverseHook = new MultiverseHook();
        }
        if (getServer().getPluginManager().isPluginEnabled("ProtocolAPI")) {
            Verbose.info("Connecting with ProtocolAPI..");
            protocolHook = new ProtocolHook();
        }
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            Verbose.info("Connecting with HolographicDisplays..");
            holoHook = new HoloHook();
        }
        if (getServer().getPluginManager().isPluginEnabled("NoteBlockAPI")) {
            Verbose.info("Connecting with NoteBlockAPI..");
            noteHook = new NoteHook();
        }
        if (Option.BUNGEECORD) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // ----- Registry -----

        registry = new Registry();

        registry.register(new DefaultGamemode());
        registry.register(new SpectatorGamemode());
        registry.registerType(new DefaultStyleType());

        for (String style : Option.STYLES) {
            registry.getStyleType("default").addConfigStyle(style);
        }

        // ----- SQL and data -----

        if (Option.SQL) {
            database = new Database();
            database.connect(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB, Option.SQL_USERNAME, Option.SQL_PASSWORD);
        }
        ParkourUser.initHighScores();

        // ----- Events -----

        this.getServer().getPluginManager().registerEvents(new Handler(), this);
        new InventoryBuilder.ClickHandler(this);

        // ----- Update checker -----

        if (Option.UPDATER) {
            UpdateChecker checker = new UpdateChecker();
            Tasks.syncRepeat(checker::check, 8 * 72000); // 8 hours
        }

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_logs", () -> Boolean.toString(Option.GAMELOGS)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Option.LANGUAGES.size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.JOIN_COUNT;
            ParkourUser.JOIN_COUNT = 0;
            return joins;
        }));
        long time = Tasks.end("load");

        Verbose.info("Loaded WITP in " + time + "ms!");
    }

    @Override
    public void onDisable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            try {
                ParkourUser.unregister(user, true, false, false);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while unregistering");
            }
        }

        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
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

    private void addCommand(String name, BukkitCommand wrapper) {
        PluginCommand command = getCommand(name);

        if (command == null) {
            throw new IllegalStateException("Command is null");
        }

        command.setExecutor(wrapper);
        command.setTabCompleter(wrapper);
    }

    // Static stuff

    public static @Nullable MultiverseHook getMultiverseHook() {
        return multiverseHook;
    }

    public static @Nullable PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static @Nullable NoteHook getNoteHook() {
        return noteHook;
    }

    public static HoloHook getHoloHook() {
        return holoHook;
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

    public static WITP getInstance() {
        return instance;
    }
}
