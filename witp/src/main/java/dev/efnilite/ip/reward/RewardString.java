package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.hook.VaultHook;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 */
// todo add cross-server support
// todo mode-specific way of saving one-time-rewards
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

        Mode mode = Registry.getModes().stream()
                .filter(m -> this.string.contains("%s:".formatted(m.getName().toLowerCase())))
                .findFirst()
                .orElse(Modes.DEFAULT);

        if (player.session.generator.getMode() != mode) {
            return;
        }

        string = string.replaceFirst("%s:".formatted(mode.getName().toLowerCase()), "");

        // check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", player.getName());
        }

        // check for extra data
        if (string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            player.previousData.onLeave.add(new RewardString(string));
            return;
        }

        // check for things to perform
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            player.send(string);

        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            try {
                VaultHook.deposit(player.player, Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                IP.logging().stack("Error while trying to process Vault reward", "check your rewards file for incorrect numbers", ex);
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }
    }
}