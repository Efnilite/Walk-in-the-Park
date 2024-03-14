package dev.efnilite.ip;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.PAPIHook;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.mode.SpectatorMode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.session.SessionChat;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.bstats.bukkit.Metrics;
import dev.efnilite.vilib.bstats.charts.SimplePie;
import dev.efnilite.vilib.bstats.charts.SingleLineChart;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.elevator.GitElevator;
import dev.efnilite.vilib.util.elevator.VersionComparator;
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

    private static Logging logging;
    private static IP instance;

    @Nullable
    private static PAPIHook placeholderHook;

    @Override
    public void onLoad() {
        instance = this;
        logging = new Logging(this);
    }

    @Override
    public void enable() {
        // ----- Configurations -----

        Config.reload(true);

        // ----- Registry -----

        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());

        Modes.init();
        Menu.init(this);

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging.info("Connecting with Holographic Displays...");
            HoloHook.init();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Connecting with PlaceholderAPI...");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            logging.info("Connecting with BungeeCord..");
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // ----- Worlds -----

        if (Config.CONFIG.getBoolean("joining")) {
            WorldManager.create();
        }

        // ----- Events -----

        registerListener(new Events());
        registerListener(new SessionChat());
        registerCommand("ip", new Command());

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.locales.size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.joinCount;
            ParkourUser.joinCount = 0;
            return joins;
        }));

        logging.info("Loaded IP!");
    }

    @Override
    public void disable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            ParkourUser.leave(user);
        }

        // write all IP gamemodes
        Modes.DEFAULT.getLeaderboard().write(false);

        Storage.close();
        WorldManager.delete();
    }

    @Override
    @NotNull
    public GitElevator getElevator() {
        return new GitElevator("Efnilite/Walk-in-the-Park",
                this,
                VersionComparator.FROM_SEMANTIC,
                Config.CONFIG.getBoolean("auto-updater"));
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }

    /**
     * @return This plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return logging;
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
}