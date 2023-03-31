package dev.efnilite.ip;

import dev.efnilite.ip.api.Modes;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.SpectatorMode;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.PAPIHook;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.session.SessionChat;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.storage.StorageDisk;
import dev.efnilite.ip.storage.StorageSQL;
import dev.efnilite.ip.style.DefaultStyleType;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.lib.bstats.bukkit.Metrics;
import dev.efnilite.vilib.lib.bstats.charts.SimplePie;
import dev.efnilite.vilib.lib.bstats.charts.SingleLineChart;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.elevator.GitElevator;
import dev.efnilite.vilib.util.elevator.VersionComparator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Main class of Infinite Parkour
 *
 * @author Efnilite
 * Copyright (c) 2020-2023
 */
public final class IP extends ViPlugin {

    public static final String NAME = "<#FF6464><bold>Infinite Parkour<reset>";
    public static final String PREFIX = NAME + " <dark_gray>» <gray>";
    public static final String REQUIRED_VILIB_VERSION = "1.1.0";

    private static IP instance;
    private static Registry registry;
    private static Storage storage;

    @Nullable
    private static PAPIHook placeholderHook;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void enable() {
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

        Config.reload();
        Option.init(true);

        // ----- SQL and data -----

        storage = Option.SQL ? new StorageSQL() : new StorageDisk();

        // ----- Registry -----

        registry = new Registry();

        registry.register(new DefaultMode());
        registry.register(new SpectatorMode());
        registry.registerType(new DefaultStyleType());

        registry.getStyleType("default").addConfigStyles("styles.list", Config.CONFIG.fileConfiguration);

        Modes.init();

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging().info("Connecting with Holographic Displays...");
            HoloHook.init();
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging().info("Connecting with PlaceholderAPI...");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Option.BUNGEECORD) {
            logging().info("Connecting with BungeeCord..");
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // ----- Worlds -----

        if (Option.JOINING) {
            WorldManager.create();
        }

        // ----- Events -----

        registerListener(new Handler());
        registerListener(new SessionChat());
        registerCommand("ip", new ParkourCommand());

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.locales.size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.JOIN_COUNT;
            ParkourUser.JOIN_COUNT = 0;
            return joins;
        }));

        logging().info("Loaded IP in " + Time.timerEnd("load") + "ms!");
    }

    @Override
    public void disable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            ParkourUser.unregister(user, true, false, false);
        }

        // write all IP gamemodes
        Modes.DEFAULT.getLeaderboard().write(false);

        World world = Bukkit.getWorld(Option.WORLD_NAME);
        if (world != null) {
            for (Player player : world.getPlayers()) {
                player.kickPlayer("Server is restarting");
            }
        }

        storage.close();
        WorldManager.delete();
    }

    @Override
    @NotNull
    public GitElevator getElevator() {
        return new GitElevator("Efnilite/Walk-in-the-Park", this, VersionComparator.FROM_SEMANTIC, Option.AUTO_UPDATER);
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(getPlugin().getDataFolder(), child);
    }

    /**
     * @return This plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return getPlugin().logging;
    }

    /**
     * @return The plugin instance.
     */
    public static IP getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static Registry getRegistry() {
        return registry;
    }

    public static Storage getStorage() {
        return storage;
    }
}