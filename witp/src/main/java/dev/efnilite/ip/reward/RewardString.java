package dev.efnilite.ip.reward;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.util.Logging;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 *
 * @author Efnilite
 */
public class RewardString {

    /**
     * The string, as read from rewards-v2.yml
     */
    private final String string;

    public RewardString(@NotNull String string) {
        this.string = string;
    }

    /**
     * Parses and executes this reward
     *
     * @param   player
     *          The player to which to give this reward to
     */
    public void execute(@NotNull ParkourPlayer player) {
        if (string.isEmpty()) {
            return;
        }
        String string = this.string;

        // Check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", player.getName());
        }

        if (string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            player.getPreviousData().addReward(new RewardString(string));
            return;
        }

        // Check for command types
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            player.send(string);

        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            try {
                Util.depositPlayer(player.getPlayer(), Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                Logging.stack(string + " is not a valid money reward", "Check your rewards-v2.yml file for incorrect numbers");
            }

        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }
    }
}