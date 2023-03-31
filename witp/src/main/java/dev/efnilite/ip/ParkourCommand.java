package dev.efnilite.ip;

import dev.efnilite.ip.api.Mode;
import dev.efnilite.ip.api.Modes;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
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
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.Version;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.*;

import static dev.efnilite.ip.util.Util.send;

@SuppressWarnings("deprecation")
public class ParkourCommand extends ViCommand {

    public static final HashMap<Player, Location[]> selections = new HashMap<>();

    private static final ItemStack wand;

    static {
        wand = new Item(Material.GOLDEN_AXE, "<red><bold>Schematic Wand")
                .lore("<gray>Left click: first position", "<gray>Right click: second position")
                .build();
    }

    private List<Block> getBlocks(Location minL, Location maxL) {
        World w = minL.getWorld();
        List<Block> add = new ArrayList<>();
        Location location = new Location(w, 0, 0, 0);

        for (int x = minL.getBlockX(); x <= maxL.getBlockX(); x++) {
            for (int y = minL.getBlockY(); y <= maxL.getBlockY(); y++) {
                for (int z = minL.getBlockZ(); z <= maxL.getBlockZ(); z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    if (location.getBlock().getType() != Material.AIR) {
                        add.add(location.clone().getBlock());
                    }
                }
            }
        }
        return add;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String defaultLocale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);

        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            player = null;
        }

        if (args.length == 0) {
            // Main menu
            if (player == null) {
                sendHelpMessages(sender);
            } else if (ParkourOption.MAIN.mayPerform(player)) {
                Menus.MAIN.open(player);
            }
            return true;
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "t" -> {
                    Location l = player.getLocation();

                    Schematic.create().save("hello", l.clone().subtract(50, 50, 50), l.clone().add(50, 50, 50));
                }

                // Help menu
                case "help" -> {
                    sendHelpMessages(sender);
                    return true;
                }
                case "reload" -> {
                    if (!cooldown(sender, "reload", 2500)) {
                        return true;
                    }
                    if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                        sender.sendMessage(defaultLocale, "other.no_do");
                        return true;
                    }
                    Time.timerStart("reloadIP");
                    send(sender, IP.PREFIX + "Reloading config files..");

                    Config.reload();
                    Option.init(false);

                    send(sender, IP.PREFIX + "Reloaded all config files in " + Time.timerEnd("reloadIP") + "ms!");
                    return true;
                }
            }
            if (player == null) {
                return true;
            }
            switch (args[0]) {
                case "leaderboard" -> {
                    player.performCommand("ip leaderboard invalid");
                    return true;
                }
                case "join" -> {
                    if (!cooldown(sender, "join", 2500)) {
                        return true;
                    }

                    if (!ParkourOption.JOIN.mayPerform(player)) {
                        send(sender, Locales.getString(defaultLocale, "other.no_do"));
                        return true;
                    }

                    ParkourUser user = ParkourUser.getUser(player);
                    if (user != null) {
                        return true;
                    }

                    Modes.DEFAULT.create(player);
                    return true;
                }
                case "leave" -> {
                    if (!cooldown(sender, "leave", 2500)) {
                        return true;
                    }
                    ParkourUser.leave(player);

                    return true;
                }
                case "menu", "main" -> {
                    if (!ParkourOption.MAIN.mayPerform(player)) {
                        Menus.MAIN.open(player);
                    }
                    return true;
                }
                case "play" -> {
                    if (!ParkourOption.PLAY.mayPerform(player)) {
                        return true;
                    }
                    Menus.PLAY.open(player);

                    return true;
                }
                case "schematic" -> {
                    if (!player.hasPermission(ParkourOption.SCHEMATIC.permission)) { // default players shouldn't have access even if perms are disabled
                        send(sender, Locales.getString(defaultLocale, "other.no_do"));
                        return true;
                    }
                    send(player, "<dark_gray>----------- <dark_red><bold>Schematics <dark_gray>-----------");
                    send(player, "");
                    send(player, "<gray>Welcome to the schematic creating section.");
                    send(player, "<gray>You can use the following commands:");
                    if (Version.isHigherOrEqual(Version.V1_14)) {
                        send(player, "<red>/ip schematic wand <dark_gray>- <gray>Get the schematic wand");
                    }
                    send(player, "<red>/ip schematic pos1 <dark_gray>- <gray>Set the first position of your selection");
                    send(player, "<red>/ip schematic pos2 <dark_gray>- <gray>Set the second position of your selection");
                    send(player, "<red>/ip schematic save <dark_gray>- <gray>Save your selection to a schematic file");
                    send(player, "<red>/ip schematic paste <file> <dark_gray>- <gray>Paste a schematic file");
                    send(player, "");
                    send(player, "<dark_gray><underlined>Have any questions or need help? Join the Discord!");
                    return true;
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") && sender instanceof Player) {
                if (!cooldown(sender, "join", 2500)) {
                    return true;
                }

                if (!ParkourOption.JOIN.mayPerform(player)) {
                    send(sender, Locales.getString(defaultLocale, "other.no_do"));
                    return true;
                }

                String type = args[1]; // get mode from second arg
                Mode gamemode = IP.getRegistry().getMode(type);
                ParkourPlayer sessionOwner = ParkourPlayer.getPlayer(Bukkit.getPlayer(type));

                if (gamemode == null) {
                    if (sessionOwner == null) {
                        send(sender, IP.PREFIX + "Unknown player! Try typing the name again."); // could not find, so go to default
                    } else {
                        ParkourUser user = ParkourUser.getUser(player);
                        Session session = sessionOwner.session;
                        if (user != null && user.session == session) {
                            return true;
                        }

                        if (session.isAcceptingPlayers.apply(session)) {
                            Modes.DEFAULT.create(player);
                        } else {
                            Modes.SPECTATOR.create(player, session);
                        }
                    }
                } else {
                    gamemode.click(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("schematic") && player != null && player.hasPermission(ParkourOption.ADMIN.permission)) {

                Location playerLocation = player.getLocation();
                Location[] existingSelection = selections.get(player);

                switch (args[1].toLowerCase()) {
                    case "wand" -> {
                        if (Version.isHigherOrEqual(Version.V1_14)) {
                            player.getInventory().addItem(wand);

                            send(player, "<dark_gray>----------- <dark_red><bold>Schematics <dark_gray>-----------");
                            send(player, "<gray>Use your IP Schematic Wand to easily select schematics.");
                            send(player, "<gray>Use <dark_gray>left click<gray> to set the first position, and <dark_gray>right click <gray>for the second!");
                            send(player, "<gray>If you can't place a block and need to set a position mid-air, use <dark_gray>the pos commands <gray>instead.");
                        }
                        return true;
                    }
                    case "pos1" -> {
                        send(player, IP.PREFIX + "Position 1 was set to " + Locations.toString(playerLocation, true));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{playerLocation, null});
                            return true;
                        }

                        selections.put(player, new Location[]{playerLocation, existingSelection[1]});

                        Particles.box(BoundingBox.of(playerLocation, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        return true;
                    }
                    case "pos2" -> {
                        send(player, IP.PREFIX + "Position 2 was set to " + Locations.toString(playerLocation, true));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{null, playerLocation});
                            return true;
                        }

                        selections.put(player, new Location[]{existingSelection[0], playerLocation});

                        Particles.box(BoundingBox.of(existingSelection[0], playerLocation), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        return true;
                    }
                    case "save" -> {
                        if (!cooldown(sender, "schematic-save", 2500)) {
                            return true;
                        }

                        if (existingSelection == null || existingSelection[0] == null || existingSelection[1] == null) {
                            send(player, "<dark_gray>----------- <dark_red><bold>Schematics <dark_gray>-----------");
                            send(player, "<gray>Your schematic isn't complete yet.");
                            send(player, "<gray>Be sure to set the first and second position!");
                            return true;
                        }

                        String code = UUID.randomUUID().toString().split("-")[0];

                        send(player, "<dark_gray>----------- <dark_red><bold>Schematics <dark_gray>-----------");
                        send(player, "<gray>Your schematic is being saved..");
                        send(player, "<gray>Your schematic will be generated with random number code <red>'" + code + "'<gray>!");
                        send(player, "<gray>You can change the file name to whatever number you like.");
                        send(player, "<dark_gray>Be sure to add this schematic to &r<dark_gray>schematics.yml!");

                        Schematic.create().save(IP.getInFolder("schematics/parkour-%s".formatted(code)), existingSelection[0], existingSelection[1]);
                        return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("forcejoin") && args[1] != null && sender.hasPermission(ParkourOption.ADMIN.permission)) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        Modes.DEFAULT.create(other);
                    }
                    send(sender, IP.PREFIX + "Successfully force joined everyone!");
                    return true;
                }

                if (args[1].equalsIgnoreCase("nearest")) {
                    Player closest = null;
                    double distance = Double.MAX_VALUE;

                    // if player is found get location from player
                    // if no player is found, get location from command block
                    // if no command block is found, return null
                    Location from = sender instanceof Player ? player.getLocation() : (sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getLocation() : null);

                    if (from == null || from.getWorld() == null) {
                        return true;
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
                        return true;
                    }

                    send(sender, IP.PREFIX + "Successfully force joined " + closest.getName() + "!");
                    Modes.DEFAULT.create(closest);
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                Modes.DEFAULT.create(other);
                return true;
            } else if (args[0].equalsIgnoreCase("forceleave") && args[1] != null && sender.hasPermission(ParkourOption.ADMIN.permission)) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (ParkourPlayer other : ParkourUser.getActivePlayers()) {
                        ParkourUser.leave(other);
                    }
                    send(sender, IP.PREFIX + "Successfully force kicked everyone!");
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    send(sender, IP.PREFIX + "That player isn't currently playing!");
                    return true;
                }

                ParkourUser.leave(user);
                return true;
            } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                if (!cooldown(sender, "recoverinventory", 2500)) {
                    return true;
                }
                Player arg1 = Bukkit.getPlayer(args[1]);
                if (arg1 == null) {
                    send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                InventoryData data = new InventoryData(arg1);
                data.readFile(readData -> {
                    if (readData != null) {
                        send(sender, IP.PREFIX + "Successfully recovered the inventory of " + arg1.getName() + " from their file");
                        if (readData.apply(true)) {
                            send(sender, IP.PREFIX + "Giving " + arg1.getName() + " their items now...");
                        } else {
                            send(sender, IP.PREFIX + "<red>There was an error decoding an item of " + arg1.getName());
                            send(sender, IP.PREFIX + "" + arg1.getName() + "'s file has been manually edited or has no saved inventory. " + "Check the console for more information.");
                        }
                    } else {
                        send(sender, IP.PREFIX + "<red>There was an error recovering the inventory of " + arg1.getName() + " from their file");
                        send(sender, IP.PREFIX + arg1.getName() + " has no saved inventory or there was an error. Check the console.");
                    }
                });
            } else if (args[0].equalsIgnoreCase("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                if (!cooldown(sender, "reset", 2500)) {
                    return true;
                }

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (Mode gamemode : IP.getRegistry().getModes()) {
                        Leaderboard leaderboard = gamemode.getLeaderboard();

                        if (leaderboard == null) {
                            continue;
                        }

                        leaderboard.resetAll();
                    }

                    send(sender, IP.PREFIX + "Successfully reset all high scores in memory and the files.");
                } else {
                    String name = null;
                    UUID uuid = null;

                    // Check online players
                    Player online = Bukkit.getPlayerExact(args[1]);
                    if (online != null) {
                        name = online.getName();
                        uuid = online.getUniqueId();
                    }

                    // Check uuid
                    if (args[1].contains("-")) {
                        uuid = UUID.fromString(args[1]);
                    }

                    // Check offline player
                    if (uuid == null) {
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                        name = offline.getName();
                        uuid = offline.getUniqueId();
                    }

                    UUID finalUuid = uuid;
                    String finalName = name;

                    for (Mode gamemode : IP.getRegistry().getModes()) {
                        Leaderboard leaderboard = gamemode.getLeaderboard();

                        if (leaderboard == null) {
                            continue;
                        }

                        leaderboard.remove(finalUuid);
                    }

                    send(sender, IP.PREFIX + "Successfully reset the high score of " + finalName + " in memory and the files.");
                }

                return true;
            } else if (args[0].equalsIgnoreCase("leaderboard")) {

                // check permissions
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(defaultLocale, "other.no_do"));
                    return true;
                }

                Mode gamemode = IP.getRegistry().getMode(args[1].toLowerCase());

                // if found gamemode is null, return to default
                if (gamemode == null) {
                    Menus.LEADERBOARDS.open(player);
                } else {
                    Menus.SINGLE_LEADERBOARD.open(player, gamemode, SingleLeaderboardMenu.Sort.SCORE);
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("schematic") && player != null && player.hasPermission(ParkourOption.ADMIN.permission)) {
                if (args[1].equalsIgnoreCase("paste")) {
                    String name = args[2];
                    Schematic schematic = Schematics.CACHE.get(name);
                    if (schematic == null) {
                        send(sender, IP.PREFIX + "Couldn't find " + name);
                        return true;
                    }

                    schematic.paste(player.getLocation());
                    send(sender, IP.PREFIX + "Pasted schematic " + name);
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
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
                completions.add("leaderboards");
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
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.add("everyone");
                for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
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
        } else {
            return Collections.emptyList();
        }
    }

    public static void sendHelpMessages(CommandSender sender) {
        send(sender, "");
        send(sender, "<dark_gray><strikethrough>---------------<reset> " + IP.NAME + " <dark_gray><strikethrough>---------------<reset>");
        send(sender, "");
        send(sender, "<gray>/parkour <dark_gray>- Main command");
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, "<gray>/parkour join [mode] <dark_gray>- Join the default gamemode or specify a mode.");
            send(sender, "<gray>/parkour leave <dark_gray>- Leave the game on this server");
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission)) {
            send(sender, "<gray>/parkour menu <dark_gray>- Open the menu");
        }
        if (sender.hasPermission(ParkourOption.PLAY.permission)) {
            send(sender, "<gray>/parkour play <dark_gray>- Mode selection menu");
        }
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission)) {
            send(sender, "<gray>/parkour leaderboard [type]<dark_gray>- Open the leaderboard of a gamemode");
        }
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/ip schematic <dark_gray>- Create a schematic");
            send(sender, "<gray>/ip reload <dark_gray>- Reloads the messages-v3.yml file");
            send(sender, "<gray>/ip reset <everyone/player> <dark_gray>- Resets all highscores. <red>This can't be recovered!");
            send(sender, "<gray>/ip forcejoin <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to join");
            send(sender, "<gray>/ip forceleave <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to leave");
            send(sender, "<gray>/ip recoverinventory <player> <dark_gray>- Recover a player's saved inventory." + " <red>Useful for recovering data after server crashes or errors when leaving.");
        }
        send(sender, "");

    }
}