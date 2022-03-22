package dev.efnilite.witp.reward;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.player.ParkourPlayer;
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
     * Executes a RewardString while checking for the "leave:" parameter.
     *
     * @see #execute(ParkourPlayer, boolean)
     *
     * @param   player
     *          The player to which to give this reward to
     */
    public void execute(@NotNull ParkourPlayer player) {
        this.execute(player, true);
    }

    /**
     * Parses and executes this reward
     *
     * @param   player
     *          The player to which to give this reward to
     *
     * @param   checkExecuteNow
     *          Should it check for the "leave:" parameter?
     */
    public void execute(@NotNull ParkourPlayer player, boolean checkExecuteNow) {
        if (string.isEmpty()) {
            return;
        }
        String string = this.string;

        // Check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", player.getPlayer().getName());
        }

        boolean executeNow = true; // should the command be executed now or later?
        if (checkExecuteNow && string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            executeNow = false;
        }

        // Check for command types
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            if (executeNow) {
                player.send(string);
            } else {
                player.getPreviousData().addReward(this);
            }
        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            if (executeNow) {
                try {
                    Util.depositPlayer(player.getPlayer(), Double.parseDouble(string));
                } catch (NumberFormatException ex) {
                    Logging.stack(string + " is not a valid money reward", "Check your rewards.yml file for incorrect numbers");
                }
            } else {
                player.getPreviousData().addReward(this);
            }
        } else {
            if (executeNow) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
            } else {
                player.getPreviousData().addReward(this);
            }
        }
    }
}