package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.hook.VaultHook;
import dev.efnilite.ip.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 *
 * @author Efnilite
 */
// todo add cross-server support
public record RewardString(@NotNull String string) {

    /**
     * Parses and executes this reward
     *
     * @param player The player to which to give this reward to
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

        // Check for extra data
        if (string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            player.previousData.addReward(new RewardString(string));
            return;
        }

        // Check for things to perform
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            player.send(string);

        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            try {
                VaultHook.deposit(player.player, Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                IP.logging().stack(string + " is not a valid money reward", "Check your rewards-v2.yml file for incorrect numbers");
            }

        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }
    }
}