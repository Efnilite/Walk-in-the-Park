package dev.efnilite.ip;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.mode.MultiMode;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.InventoryData;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.command.ViCommand;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.efnilite.ip.util.Util.send;

@SuppressWarnings("deprecation")
public class ParkourCommand extends ViCommand {

    public static final HashMap<Player, Location[]> selections = new HashMap<>();

    private static final ItemStack WAND = new Item(Material.GOLDEN_AXE, "<red><bold>Schematic Wand")
            .lore("<gray>Left click: first position", "<gray>Right click: second position")
            .build();

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        switch (args.length) {
            case 0 -> handle0Args(sender, player);
            case 1 -> handle1Args(args[0], sender, player);
            case 2 -> handle2Args(args[0], args[1], sender, player);
            case 3 -> handle3Args(args[0], args[1], args[2], sender, player);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
                if (ParkourOption.JOIN.mayPerform(sender)) {
                    completions.add("join");
                    completions.add("leave");
                }
                if (ParkourOption.MAIN.mayPerform(sender)) {
                    completions.add("menu");
                }
                if (ParkourOption.PLAY.mayPerform(sender)) {
                    completions.add("play");
                }
                if (ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                    completions.add("leaderboard");
                }
                if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("schematic");
                    completions.add("reload");
                    completions.add("forcejoin");
                    completions.add("forceleave");
                    completions.add("reset");
                    completions.add("recoverinventory");
                }
                return completions(args[0], completions);
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("join") && sender.hasPermission(ParkourOption.JOIN.permission)) {
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("schematic") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "paste"));
                } else if (args[0].equalsIgnoreCase("forcejoin") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("nearest");
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("forceleave") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                }
                return completions(args[1], completions);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    private void handle0Args(@NotNull CommandSender sender, @Nullable Player player) {
        if (player != null && ParkourOption.MAIN.mayPerform(player)) {
            Menus.MAIN.open(player);
            return;
        }
        sendHelpMessages(sender);
    }

    private void sendHelpMessages(CommandSender sender) {
        send(sender, "");
        send(sender, "<dark_gray><strikethrough>---------------<reset> %s <dark_gray><strikethrough>---------------<reset>".formatted(IP.NAME));
        send(sender, "");
        send(sender, "<gray>/parkour <dark_gray>- Main command");
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, "<gray>/parkour join [mode] <dark_gray>- Join the default mode or specify one.");
            send(sender, "<gray>/parkour leave <dark_gray>- Leave the game on this server");
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission)) {
            send(sender, "<gray>/parkour menu <dark_gray>- Open the menu");
        }
        if (sender.hasPermission(ParkourOption.PLAY.permission)) {
            send(sender, "<gray>/parkour play <dark_gray>- Mode selection menu");
        }
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission)) {
            send(sender, "<gray>/parkour leaderboard [type]<dark_gray>- Open the leaderboard of a mode");
        }
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/ip schematic <dark_gray>- Create a schematic");
            send(sender, "<gray>/ip reload <dark_gray>- Reloads the messages-v3.yml file");
            send(sender, "<gray>/ip reset <everyone/player> <dark_gray>- Resets all high scores. <red>This can't be recovered!");
            send(sender, "<gray>/ip forcejoin <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to join");
            send(sender, "<gray>/ip forceleave <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to leave");
            send(sender, "<gray>/ip recoverinventory <player> <dark_gray>- Recover a player's saved inventory. <red>Useful for recovering data after server crashes or errors when leaving.");
        }
        send(sender, "");
    }

    private void handle1Args(@NotNull String arg, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg.toLowerCase()) {
            case "help" -> sendHelpMessages(sender);
            case "reload" -> {
                if (!cooldown(sender, "reload", 2500)) {
                    return;
                }
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Config.reload(false);

                send(sender, "%sReloaded config files.".formatted(IP.PREFIX));
            }
        }

        if (player == null) {
            return;
        }

        switch (arg.toLowerCase()) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500)) {
                    return;
                }

                if (!ParkourOption.JOIN.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    return;
                }

                Modes.DEFAULT.create(player);
            }
            case "play" -> {
                if (ParkourOption.PLAY.mayPerform(player)) {
                    Menus.PLAY.open(player);
                }
            }
            case "leave" -> {
                if (!cooldown(sender, "leave", 2500)) {
                    return;
                }
                ParkourUser.leave(player);
            }
            case "menu", "main" -> {
                if (!ParkourOption.MAIN.mayPerform(player)) {
                    Menus.MAIN.open(player);
                }
            }
            case "leaderboard" -> player.performCommand("ip leaderboard invalid");
            case "schematic" -> {
                if (!player.hasPermission(ParkourOption.ADMIN.permission)) { // default players shouldn't have access even if perms are disabled
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                send(player, "");
                send(player, "<red>/ip schematic wand <dark_gray>- <gray>Get the schematic wand");
                send(player, "<red>/ip schematic pos1 <dark_gray>- <gray>Set the first position of your selection");
                send(player, "<red>/ip schematic pos2 <dark_gray>- <gray>Set the second position of your selection");
                send(player, "<red>/ip schematic save <dark_gray>- <gray>Save your selection to a schematic file");
                send(player, "<red>/ip schematic paste <file> <dark_gray>- <gray>Paste a schematic file");
                send(player, "");
                send(player, "<dark_gray><underlined>Have any questions or need help? Join the Discord.");
            }
        }
    }

    private void handle2Args(@NotNull String arg1, @NotNull String arg2, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg1.toLowerCase()) {
            case "forcejoin" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    Bukkit.getOnlinePlayers().forEach(other -> Modes.DEFAULT.create(other));
                    send(sender, IP.PREFIX + "Successfully force joined everyone!");
                    return;
                }

                if (arg2.equalsIgnoreCase("nearest")) {
                    Player closest = null;
                    double distance = Double.MAX_VALUE;

                    // if player is found get location from player
                    // if no player is found, get location from command block
                    // if no command block is found, return null
                    Location from = sender instanceof Player ? ((Player) sender).getLocation() : (sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getLocation() : null);

                    if (from == null || from.getWorld() == null) {
                        return;
                    }

                    // get the closest player
                    for (Player p : from.getWorld().getPlayers()) {
                        double d = p.getLocation().distance(from);

                        if (d < distance) {
                            distance = d;
                            closest = p;
                        }
                    }

                    // no closest player found
                    if (closest == null) {
                        return;
                    }

                    send(sender, IP.PREFIX + "Successfully force joined " + closest.getName() + "!");
                    Modes.DEFAULT.create(closest);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return;
                }

                Modes.DEFAULT.create(other);
            }
            case "forceleave" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    ParkourPlayer.getPlayers().forEach(ParkourUser::leave);
                    send(sender, IP.PREFIX + "Successfully force kicked everyone!");
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    send(sender, IP.PREFIX + "That player isn't currently playing!");
                    return;
                }

                ParkourUser.leave(user);
            }
            case "reset" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission) || !cooldown(sender, "reset", 2500)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    for (Mode mode : Registry.getModes()) {
                        Leaderboard leaderboard = mode.getLeaderboard();

                        if (leaderboard == null) {
                            continue;
                        }

                        leaderboard.resetAll();
                        leaderboard.write(true);
                    }

                    send(sender, IP.PREFIX + "Successfully reset all high scores in memory and the files.");
                    return;
                }
                String name = null;
                UUID uuid = null;

                // Check online players
                Player online = Bukkit.getPlayerExact(arg2);
                if (online != null) {
                    name = online.getName();
                    uuid = online.getUniqueId();
                }

                // Check uuid
                if (arg2.contains("-")) {
                    uuid = UUID.fromString(arg2);
                }

                // Check offline player
                if (uuid == null) {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(arg2);
                    name = offline.getName();
                    uuid = offline.getUniqueId();
                }

                UUID finalUuid = uuid;
                String finalName = name;

                for (Mode mode : Registry.getModes()) {
                    Leaderboard leaderboard = mode.getLeaderboard();

                    if (leaderboard == null) {
                        continue;
                    }

                    leaderboard.remove(finalUuid);
                    leaderboard.write(true);
                }

                send(sender, IP.PREFIX + "Successfully reset the high score of " + finalName + " in memory and the files.");
            }
            case "recoverinventory" -> {
                if (!cooldown(sender, "recoverinventory", 2500) || !sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return;
                }

                new InventoryData(other).load(result -> {
                    if (result != null) {
                        send(sender, "%sSuccessfully recovered the inventory of %s from their file".formatted(IP.PREFIX, other.getName()));
                        send(sender, "%sGiving %s their items now...".formatted(IP.PREFIX, other.getName()));
                    } else {
                        send(sender, "%s<red>There was an error recovering the inventory of %s from their file".formatted(IP.PREFIX, other.getName()));
                        send(sender, "%s%s has no saved inventory or there was an error. Check the console.".formatted(IP.PREFIX, other.getName()));
                    }
                });
            }
        }

        if (player == null) {
            return;
        }

        switch (arg1) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500) || !ParkourOption.JOIN.mayPerform(player)) {
                    return;
                }

                Mode mode = Registry.getMode(arg2);

                if (mode != null) {
                    mode.create(player);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);

                if (other == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(IP.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourPlayer parkourPlayer = ParkourPlayer.getPlayer(other);

                if (parkourPlayer == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(IP.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                Session session = parkourPlayer.session;
                if (user != null && user.session == session) { // already in same session
                    return;
                }

                if (session.isAcceptingPlayers()) {
                    ((MultiMode) session.generator.getMode()).join(player, session);
                } else {
                    Modes.SPECTATOR.create(player, session);
                }
            }
            case "leaderboard" -> {
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Mode mode = Registry.getMode(arg2.toLowerCase());

                // if found gamemode is null, return to default
                if (mode == null) {
                    Menus.LEADERBOARDS.open(player);
                } else {
                    Menus.SINGLE_LEADERBOARD.open(player, mode, SingleLeaderboardMenu.Sort.SCORE);
                }
            }
            case "schematic" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Location playerLocation = player.getLocation();
                Location[] existingSelection = selections.get(player);

                switch (arg2.toLowerCase()) {
                    case "wand" -> {
                        player.getInventory().addItem(WAND);

                        send(player, "<dark_gray>----------- <dark_red><bold>Schematics <reset><dark_gray>-----------");
                        send(player, "<gray><red>Left click<gray> -> set first position | <red>Right click<gray> -> set second position");
                        send(player, "<gray>If you can't place a block and need to set a position mid-air, use <dark_gray>/ip schematic pos1/pos2 <gray>instead.");
                    }
                    case "pos1" -> {
                        send(player, "%sPosition 1 was set to %s".formatted(IP.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{playerLocation, null});
                            return;
                        }

                        selections.put(player, new Location[]{playerLocation, existingSelection[1]});

                        Particles.box(BoundingBox.of(playerLocation, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "pos2" -> {
                        send(player, "%sPosition 2 was set to %s".formatted(IP.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{null, playerLocation});
                            return;
                        }

                        selections.put(player, new Location[]{existingSelection[0], playerLocation});

                        Particles.box(BoundingBox.of(existingSelection[0], playerLocation), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "save" -> {
                        if (!cooldown(sender, "IP save schematic", 2500)) {
                            return;
                        }

                        if (existingSelection == null || existingSelection[0] == null || existingSelection[1] == null) {
                            send(player, "<dark_red><bold>Schematics <reset><gray>Your schematic isn't complete yet. Make sure you've set the first and second position.");
                            return;
                        }

                        String code = UUID.randomUUID().toString().split("-")[0];

                        send(player, ("<dark_red><bold>Schematics <reset><gray>Your schematic is being saved. It will use code <red>'%s'<gray>. " + "You can change the code to whatever you like. " + "Don't forget to add this schematic to <dark_gray>schematics.yml<gray>.").formatted(code));

                        Schematic.create().save(IP.getInFolder("schematics/parkour-%s".formatted(code)), existingSelection[0], existingSelection[1]);
                    }
                }
            }
        }
    }

    private void handle3Args(@NotNull String arg1, @NotNull String arg2, @NotNull String arg3, @NotNull CommandSender sender, @Nullable Player player) {
        if (player == null) {
            return;
        }

        if (arg1.equalsIgnoreCase("schematic")) {
            if (!arg2.equalsIgnoreCase("paste")) {
                return;
            }

            if (!player.hasPermission(ParkourOption.ADMIN.permission)) {
                return;
            }

            Schematic schematic = Schematics.CACHE.get(arg3);
            if (schematic == null) {
                send(sender, "%sCouldn't find %s".formatted(IP.PREFIX, arg3));
                return;
            }

            schematic.paste(player.getLocation());
            send(sender, "%sPasted schematic %s".formatted(IP.PREFIX, arg3));
        }
    }
}