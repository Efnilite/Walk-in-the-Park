package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.hook.VaultHook;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 */
public record Reward(@NotNull String string) {

    public void execute(Player player, Mode currentMode) {
        var parts = string.split("\\|\\|", 4);

        var time = parts[0].toLowerCase();

        if (time.equals("leave")) {
            var pp = ParkourPlayer2.as(player);

            if (pp != null) {
                pp.addReward(currentMode, new Reward("now||%s||%s||%s"
                        .formatted(parts[1], parts[2], parts[3])));
            }

            return;
        }

        var modeName = parts[1].toLowerCase();

        if (!modeName.equals("all")) {
            var mode = Registry.getMode(modeName);

            if (mode == null) {
                IP.logging().error("Invalid mode %s in rewards".formatted(modeName));
                return;
            }

            if (mode != currentMode) {
                return;
            }
        }

        var command = parts[2].toLowerCase();
        var value = parts[3].replace("%player%", player.getName());

        switch (command) {
            case "send" -> player.sendMessage(Strings.colour(value));
            case "player command" -> player.performCommand(value);
            case "console command" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
            case "vault" -> {
                try {
                    var amount = Double.parseDouble(value);

                    VaultHook.give(player, amount);
                } catch (NumberFormatException ex) {
                    IP.logging().error("Invalid numerical value %s in rewards".formatted(value));
                }
            }
            default -> IP.logging().error("Invalid command %s in rewards".formatted(command));
        }
    }
}