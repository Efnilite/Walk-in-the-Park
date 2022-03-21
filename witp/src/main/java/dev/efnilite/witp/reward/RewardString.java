package dev.efnilite.witp.reward;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 *
 * @author Efnilite
 */
public class RewardString {

    /**
     * The string, as read from rewards.yml
     */
    private final String string;

    public RewardString(@NotNull String string) {
        this.string = string;
    }

    /**
     * Parses and executes this reward
     *
     * @param   user
     *          The user, which to give this reward to
     */
    public void execute(@NotNull ParkourUser user) {
        if (string.isEmpty()) {
            return;
        }
        String string = this.string;

        // Check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", user.getPlayer().getName());
        }

        // Check for command types
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");
            user.send(string);
        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");
            try {
                Util.depositPlayer(user.getPlayer(), Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                Logging.stack(string + " is not a valid money reward", "Check your rewards.yml file for incorrect numbers");
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }
    }
}