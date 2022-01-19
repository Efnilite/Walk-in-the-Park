package dev.efnilite.witp.util.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

/**
 * Utilities for Runnables
 */
public class Tasks {

    private static Plugin plugin;
    private static HashMap<String, Long> timingKeys;

    public static void init(Plugin plugin) {
        Tasks.plugin = plugin;
        timingKeys = new HashMap<>();
    }

    public static void time(String key) {
        timingKeys.put(key, System.currentTimeMillis());
    }

    public static long end(String key) {
        long then = timingKeys.get(key);
        timingKeys.remove(key);
        return System.currentTimeMillis() - then;
    }

    public static BukkitTask syncRepeat(BukkitRunnable runnable, int interval) {
        return runnable.runTaskTimer(plugin, 0L, interval);
    }

    public static BukkitTask defaultSyncRepeat(Runnable runnable, int interval) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, 0L, interval);
    }

    public static BukkitTask asyncRepeat(Runnable runnable, int interval) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, 0L, interval);
    }

    public static BukkitTask syncTask(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static BukkitTask asyncTask(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static BukkitTask asyncDelay(Runnable runnable, int delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public static BukkitTask syncDelay(Runnable runnable, int delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }
}