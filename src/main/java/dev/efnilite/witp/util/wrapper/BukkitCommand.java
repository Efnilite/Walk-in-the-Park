package dev.efnilite.witp.util.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command wrap
 */
public abstract class BukkitCommand implements CommandExecutor, TabCompleter {

    /**
     * Execute a command
     */
    public abstract boolean execute(Player player, String[] args);

    /**
     * Get what should be suggested
     */
    public abstract List<String> tabComplete(Player player, String[] args);

    protected List<String> completions(String typed, List<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().contains(typed))
                .collect(Collectors.toList());
    }

    protected List<String> completions(String typed, String... options) {
        return Arrays.stream(options)
                .filter(option -> option.toLowerCase().contains(typed))
                .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = Bukkit.getPlayer(sender.getName());

            if (player == null) {
                return false;
            }

            return execute(player, args);
        } else {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = Bukkit.getPlayer(sender.getName());

            if (player == null) {
                return Collections.emptyList();
            }

            return tabComplete(player, args);
        } else {
            return Collections.emptyList();
        }
    }
}