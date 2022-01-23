package dev.efnilite.witp.wrapper;

import dev.efnilite.witp.command.MainCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command wrap
 */
public abstract class SimpleCommand implements CommandExecutor, TabCompleter {

    /**
     * UUID-based cooldown system
     */
    private final Map<UUID, List<CommandCooldown>> cooldowns = new HashMap<>();

    /**
     * Execute a command
     */
    public abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Get what should be suggested
     */
    public abstract List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Checks the cooldown
     *
     * @param   sender
     *          The CommandSender which may have a cooldown
     *
     * @param   arg
     *          The argument to which this cooldown applies
     *
     * @param   cooldownMs
     *          The cooldown in ms
     *
     * @return false if the cooldown is not over yet, true if it has been.
     */
    protected boolean cooldown(CommandSender sender, String arg, long cooldownMs) {
        if (sender instanceof ConsoleCommandSender) { // ignore console (has no UUID)
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        CommandCooldown cooldown = null; // the current cooldown
        List<CommandCooldown> playerCooldowns = cooldowns.get(uuid) == null ? new ArrayList<>() : cooldowns.get(uuid);
        for (CommandCooldown plCooldown : playerCooldowns) { // get the appropriate commandcooldown class
            if (plCooldown.getArg().equals(arg)) {
                cooldown = plCooldown;
                break;
            }
        }

        if (cooldown == null) { // cooldown doesnt exist yet
            playerCooldowns.add(new CommandCooldown(arg));
            cooldowns.put(uuid, playerCooldowns);
            return true;
        }

        if (System.currentTimeMillis() - cooldown.getLastExecuted() > cooldownMs) {

            playerCooldowns.remove(cooldown); // update cooldown
            playerCooldowns.add(new CommandCooldown(arg));
            cooldowns.put(uuid, playerCooldowns);

            return true;
        } else {
            MainCommand.send(sender, "&4&l> &7Please wait before using that again.");
            return false;
        }
    }

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

    public static void register(String name, SimpleCommand wrapper) {
        PluginCommand command = Bukkit.getPluginCommand(name);

        if (command == null) {
            return; // command has been overwritten, whatever lol
        }

        command.setExecutor(wrapper);
        command.setTabCompleter(wrapper);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = Bukkit.getPlayer(sender.getName());

            if (player == null) {
                return false;
            }

            return execute(player, args);
        } else if (sender instanceof ConsoleCommandSender) {
            return execute(Bukkit.getConsoleSender(), args);
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