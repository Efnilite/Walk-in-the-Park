package dev.efnilite.ip;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.hook.FloodgateHook;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.PAPIHook;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.mode.SpectatorMode;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.world.Divider;
import dev.efnilite.ip.world.World;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.bstats.bukkit.Metrics;
import dev.efnilite.vilib.bstats.charts.SimplePie;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.util.Logging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;

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

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] " + message);
        }
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
            logging.info("Registered Holographic Displays hook");
            HoloHook.init();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Registered PlaceholderAPI hook");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            logging.info("Registered BungeeCord hook");
        }

        // ----- Worlds -----

        if (Config.CONFIG.getBoolean("joining")) {
            World.create();
        }

        // ----- Events -----

        registerListener(new Events());
        registerCommand("ip", new Command());

        // ----- Metrics -----

        Metrics metrics = new Metrics(this, 9272);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.locales.size())));

        logging.info("Loaded IP!");
    }

    @Override
    public void disable() {
        try {
            for (Session generator : new HashSet<>(Divider.sections.keySet())) {
                generator.getPlayers().forEach(player -> player.leave(false, true));
            }

            // write all IP gamemodes
            Modes.DEFAULT.getLeaderboard().write(false);

            Storage.close();
            World.delete();
        } catch (Throwable ignored) {

        }
    }

    /**
     * @param player The player
     * @return true if the player is a Bedrock player, false if not.
     */
    public static boolean isBedrockPlayer(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateHook.isBedrockPlayer(player);
    }
}