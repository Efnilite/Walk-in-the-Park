package dev.efnilite.ip.util;

import dev.efnilite.ip.hook.FloodgateHook;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * General utilities
 *
 * @author Efnilite
 */
public class Util {

    public static List<String> getChildren(FileConfiguration file, String path, boolean deep) {
        ConfigurationSection section = file.getConfigurationSection(path);
        return section != null ? new ArrayList<>(section.getKeys(deep)) : Collections.emptyList();
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }

    /**
     * Gets a spiral
     *
     * @param n The number of  value
     * @return the coords of this value
     */
    // https://math.stackexchange.com/a/163101
    public static int[] spiralAt(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Invalid n bound: %d".formatted(n));
        }

        n++; // one-index
        int k = (int) Math.ceil((Math.sqrt(n) - 1) / 2);
        int t = 2 * k + 1;
        int m = t * t;
        t--;

        if (n > m - t) {
            return new int[]{k - (m - n), -k};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k, -k + (m - n)};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k + (m - n), k};
        } else {
            return new int[]{k, k - (m - n - t)};
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