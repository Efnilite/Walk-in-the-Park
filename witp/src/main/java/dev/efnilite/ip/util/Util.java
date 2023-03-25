package dev.efnilite.ip.util;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Strings;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * General utilities
 *
 * @author Efnilite
 */
public class Util {

    private static final char[] RANDOM_DIGITS = "1234567890".toCharArray();

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

    public static boolean listContains(List<String> list, String... strings) {
        for (String s : list) {
            for (String string : strings) {
                if (s.contains(string)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Random digits
     *
     * @return a string with an amount of random digits
     */
    public static String randomDigits(int amount) {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            random.append(RANDOM_DIGITS[ThreadLocalRandom.current().nextInt(RANDOM_DIGITS.length - 1)]);
        }
        return random.toString();
    }

    public static @NotNull List<String> getChildren(FileConfiguration file, String path, boolean deep) {
        ConfigurationSection section = file.getConfigurationSection(path);
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(deep));
    }

    /**
     * Returns whether the provided player is a Bedrock player.
     * This check is provided by Floodgate
     *
     * @param player The player
     * @return true if the player is a Bedrock player, false if not.
     */
    public static boolean isBedrockPlayer(Player player) {
        return IP.getFloodgateHook() != null && IP.getFloodgateHook().isBedrockPlayer(player);
    }

    /**
     * Translates any placeholders found by PlaceholderAPI
     *
     * @param player The player
     * @param string The string that will be translated
     * @return the translated string
     */
    public static String translate(Player player, String string) {
        if (IP.getPlaceholderHook() == null) {
            return string;
        }
        return PlaceholderAPI.setPlaceholders(player, string);
    }
}