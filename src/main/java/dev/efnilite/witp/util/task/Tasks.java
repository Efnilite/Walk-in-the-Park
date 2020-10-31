package dev.efnilite.witp.util.task;

import dev.efnilite.witp.WITP;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utilities for Runnables
 */
public class Tasks {

    private static final Plugin plugin;

    static {
        plugin = WITP.getInstance();
    }

    public static int syncRepeat(BukkitRunnable runnable, int interval) {
        return runnable.runTaskTimer(plugin, 0L, interval).getTaskId();
    }

    public static int asyncRepeat(BukkitRunnable runnable, int interval) {
        return runnable.runTaskTimerAsynchronously(plugin, 0L, interval).getTaskId();
    }

    public static int syncTask(BukkitRunnable runnable) {
        return runnable.runTask(plugin).getTaskId();
    }

    public static int asyncTask(BukkitRunnable runnable) {
        return runnable.runTaskAsynchronously(plugin).getTaskId();
    }

    public static int asyncDelay(BukkitRunnable runnable, int delay) {
        return runnable.runTaskLaterAsynchronously(plugin, delay).getTaskId();
    }

    public static int syncDelay(BukkitRunnable runnable, int delay) {
        return runnable.runTaskLater(plugin, delay).getTaskId();
    }
}