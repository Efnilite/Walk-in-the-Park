package dev.efnilite.ip;

import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.LeaderboardMenu;
import dev.efnilite.ip.menu.SingleplayerMenu;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.InventoryData;
import dev.efnilite.ip.schematic.RotationAngle;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.SchematicCache;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.inventory.PersistentUtil;
import dev.efnilite.vilib.chat.Message;
import dev.efnilite.vilib.command.ViCommand;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.Version;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

@SuppressWarnings("deprecation")
public class ParkourCommand extends ViCommand {

    public static final HashMap<Player, Selection> selections = new HashMap<>();
    private ItemStack wand;

    public ParkourCommand() {
        if (Version.isHigherOrEqual(Version.V1_14)) {
            wand = new Item(
                    Material.GOLDEN_AXE, "&4&lWITP Schematic Wand")
                    .lore("&7Left click: first position", "&7Right click: second position").build();
            PersistentUtil.setPersistentData(wand, "witp", PersistentDataType.STRING, "true");
        }
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0) {
            // Main menu
            if (player == null) {
                sendHelpMessages(sender);
            } else if (ParkourOption.MENU.check(player)) {
                DynamicMenu.Reg.MAIN.open(player);
            }
            return true;
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                // Help menu
                case "help" -> {
                    sendHelpMessages(sender);
                    return true;
                }
                case "reload" -> {
                    if (!cooldown(sender, "reload", 2500)) {
                        return true;
                    }
                    if (!sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(sender, "cant-do");
                        return true;
                    }
                    Time.timerStart("reloadIP");
                    Message.send(sender, IP.PREFIX + "Reloading config files..");

                    IP.getConfiguration().reload();
                    Option.init(false);

                    Message.send(sender, IP.PREFIX + "Reloaded all config files in " + Time.timerEnd("reloadIP") + "ms!");
                    return true;
                }
                case "migrate" -> {
                    if (!cooldown(sender, "migrate", 2500)) {
                        return true;
                    }
                    if (!sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(sender, "cant-do");
                        return true;
                    } else if (!Option.SQL.get()) {
                        Message.send(sender, IP.PREFIX + "You have disabled SQL support in the config!");
                        return true;
                    }
                    Time.timerStart("migrate");
                    File folder = new File(IP.getPlugin().getDataFolder() + "/players/");
                    if (!folder.exists()) {
                        folder.mkdirs();
                        return true;
                    }
                    for (File file : folder.listFiles()) {
                        FileReader reader;
                        try {
                            reader = new FileReader(file);
                        } catch (FileNotFoundException ex) {
                            IP.logging().stack("Could not find file to migrate", ex);
                            Message.send(sender, IP.PREFIX + "<red>Could not find that file, try again!");
                            return true;
                        }
                        ParkourPlayer from = IP.getGson().fromJson(reader, ParkourPlayer.class);
                        String name = file.getName();
                        from.uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                        from.save(true);
                    }
                    Message.send(sender, IP.PREFIX + "Your players' data has been migrated in " + Time.timerEnd("migrate") + "ms!");
                    return true;
                }
            }
            if (player == null) {
                return true;
            }
            switch (args[0]) {
                case "join" -> {
                    if (!cooldown(sender, "join", 2500)) {
                        return true;
                    }

                    if (!ParkourOption.JOIN.check(player)) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }

                    if (!Option.ENABLE_JOINING.get()) {
                        IP.logging().info("Player " + player.getName() + "tried joining, but parkour is disabled.");
                        return true;
                    }

                    ParkourUser user = ParkourUser.getUser(player);
                    if (user != null) {
                        return true;
                    }

                    ParkourPlayer.joinDefault(player);
                    return true;
                }
                case "leave" -> {
                    if (!cooldown(sender, "leave", 2500)) {
                        return true;
                    }
                    ParkourUser.leave(player);

                    return true;
                }
                case "menu" -> {
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    if (Option.SETTINGS_ENABLED.get() && pp != null) {
                        pp.getGenerator().menu();
                        return true;
                    }
                    return true;
                }
                case "gamemode", "gm" -> {
                    if (!ParkourOption.GAMEMODE.check(player)) {
                        return true;
                    }
                    SingleplayerMenu.open(player);

                    return true;
                }
                case "leaderboard" -> {
                    if (!ParkourOption.LEADERBOARD.check(player)) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    LeaderboardMenu.open(player);
                }
                case "schematic" -> {
                    if (!player.hasPermission(ParkourOption.SCHEMATICS.getPermission())) { // default players shouldn't have access even if perms are disabled
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    Message.send(player, "<dark_gray>----------- &4&lSchematics <dark_gray>-----------");
                    Message.send(player, "");
                    Message.send(player, "&7Welcome to the schematic creating section.");
                    Message.send(player, "&7You can use the following commands:");
                    if (Version.isHigherOrEqual(Version.V1_14)) {
                        Message.send(player, "<red>/witp schematic wand <dark_gray>- &7Get the schematic wand");
                    }
                    Message.send(player, "<red>/witp schematic pos1 <dark_gray>- &7Set the first position of your selection");
                    Message.send(player, "<red>/witp schematic pos2 <dark_gray>- &7Set the second position of your selection");
                    Message.send(player, "<red>/witp schematic save <dark_gray>- &7Save your selection to a schematic file");
                    Message.send(player, "<red>/witp schematic paste <file> <dark_gray>- &7Paste a schematic file");
                    Message.send(player, "");
                    Message.send(player, "<dark_gray>&nHave any questions or need help? Join the Discord!");
                    return true;
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") && sender instanceof Player) {
                if (!cooldown(sender, "join", 2500)) {
                    return true;
                }

                if (!ParkourOption.JOIN.check(player)) {
                    Util.sendDefaultLang(player, "cant-do");
                    return true;
                }

                if (!Option.ENABLE_JOINING.get()) {
                    IP.logging().info("Player " + player.getName() + "tried joining, but parkour is disabled.");
                    return true;
                }

                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    return true;
                }

                String type = args[1]; // get mode from second arg
                Gamemode gamemode = IP.getRegistry().getGamemode(type);
                Session session = Session.getSession(type.toUpperCase());

                if (gamemode == null) {
                    if (session == null) {
                        Gamemodes.DEFAULT.click(player); // could not find, so go to default
                    } else {
                        session.join(player);
                    }
                } else {
                    gamemode.click(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("schematic") && player != null && player.hasPermission("witp.schematic")) {
                Selection selection = selections.get(player);
                switch (args[1].toLowerCase()) {
                    case "wand" -> {
                        if (Version.isHigherOrEqual(Version.V1_14)) {
                            player.getInventory().addItem(wand);

                            Message.send(player, "<dark_gray>----------- &4&lSchematics <dark_gray>-----------");
                            Message.send(player, "&7Use your WITP Schematic Wand to easily select schematics.");
                            Message.send(player, "&7Use <dark_gray>left click&7 to set the first position, and <dark_gray>right click &7for the second!");
                            Message.send(player, "&7If you can't place a block and need to set a position mid-air, use <dark_gray>the pos commands &7instead.");
                        }
                        return true;
                    }
                    case "pos1" -> {
                        if (selections.get(player) == null) {
                            selections.put(player, new Selection(player.getLocation(), null, player.getWorld()));
                        } else {
                            Location pos1 = player.getLocation();
                            Location pos2 = selections.get(player).getPos2();
                            selections.put(player, new Selection(pos1, pos2, player.getWorld()));
                            Particles.box(BoundingBox.of(pos1, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        }
                        Message.send(player, IP.PREFIX + "Position 1 was set to " + Locations.toString(player.getLocation(), true));
                        return true;
                    }
                    case "pos2" -> {
                        if (selections.get(player) == null) {
                            selections.put(player, new Selection(null, player.getLocation(), player.getWorld()));
                        } else {
                            Location pos1 = selections.get(player).getPos1();
                            Location pos2 = player.getLocation();
                            selections.put(player, new Selection(pos1, pos2, player.getWorld()));
                            Particles.box(BoundingBox.of(pos1, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        }
                        Message.send(player, IP.PREFIX + "Position 2 was set to " + Locations.toString(player.getLocation(), true));
                        return true;
                    }
                    case "save" -> {
                        if (!cooldown(sender, "schematic-save", 2500)) {
                            return true;
                        }
                        if (selection == null || !selection.isComplete()) {
                            Message.send(player, "<dark_gray>----------- &4&lSchematics <dark_gray>-----------");
                            Message.send(player, "&7Your schematic isn't complete yet.");
                            Message.send(player, "&7Be sure to set the first and second position!");
                            return true;
                        }
                        String code = Util.randomDigits(6);
                        Message.send(player, "<dark_gray>----------- &4&lSchematics <dark_gray>-----------");
                        Message.send(player, "&7Your schematic is being saved..");
                        Message.send(player, "&7Your schematic will be generated with random number code <red>'" + code + "'&7!");
                        Message.send(player, "&7You can change the file name to whatever number you like.");
                        Message.send(player, "<dark_gray>Be sure to add this schematic to &r<dark_gray>schematics.yml!");
                        Schematic schematic = new Schematic(selection);
                        schematic.file("parkour-" + code).save(player);
                        return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("forcejoin") && args[1] != null && sender.hasPermission("witp.forcejoin")) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission("witp.forcejoin.everyone")) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        ParkourPlayer.joinDefault(other);
                    }
                    Message.send(sender, IP.PREFIX + "Succesfully force joined everyone!");
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

                    // get closest player
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

                    Message.send(sender, IP.PREFIX + "Succesfully force joined " + closest.getName() + "!");
                    ParkourPlayer.joinDefault(closest);
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    Message.send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                ParkourPlayer.joinDefault(other);
                return true;
            } else if (args[0].equalsIgnoreCase("forceleave") && args[1] != null && sender.hasPermission("witp.forceleave")) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission("witp.forceleave.everyone")) {
                    for (ParkourPlayer other : ParkourUser.getActivePlayers()) {
                        ParkourUser.leave(other);
                    }
                    Message.send(sender, IP.PREFIX + "Successfully force kicked everyone!");
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    Message.send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    Message.send(sender, IP.PREFIX + "That player isn't currently playing!");
                    return true;
                }

                ParkourUser.leave(user);
                return true;
            } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission("witp.recoverinventory")) {
                if (!cooldown(sender, "recoverinventory", 2500)) {
                    return true;
                }
                Player arg1 = Bukkit.getPlayer(args[1]);
                if (arg1 == null) {
                    Message.send(sender, IP.PREFIX + "That player isn't online!");
                    return true;
                }

                InventoryData data = new InventoryData(arg1);
                data.readFile(readData -> {
                    if (readData != null) {
                        Message.send(sender, IP.PREFIX + "Successfully recovered the inventory of " + arg1.getName() + " from their file");
                        if (readData.apply(true)) {
                            Message.send(sender, IP.PREFIX + "Giving " + arg1.getName() + " their items now...");
                        } else {
                            Message.send(sender, IP.PREFIX + "<red>There was an error decoding an item of " + arg1.getName());
                            Message.send(sender, IP.PREFIX + "" + arg1.getName() + "'s file has been manually edited or has no saved inventory. " +
                                    "Check the console for more information.");
                        }
                    } else {
                        Message.send(sender, IP.PREFIX + "<red>There was an error recovering the inventory of " + arg1.getName() + " from their file");
                        Message.send(sender, IP.PREFIX + arg1.getName() + " has no saved inventory or there was an error. Check the console.");
                    }
                });
            } else if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("witp.reset")) {
                if (!cooldown(sender, "reset", 2500)) {
                    return true;
                }

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission("witp.reset.everyone")) {
                    Task.create(IP.getPlugin()).async().execute(() -> {
                        if (ParkourUser.resetScores()) {
                            Message.send(sender, IP.PREFIX + "Successfully reset all high scores in memory and the files.");
                        } else {
                            Message.send(sender, IP.PREFIX + "<red>There was an error while trying to reset high scores.");
                        }
                    }).run();
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
                    Task.create(IP.getPlugin()).async().execute(() -> {
                        if (ParkourUser.resetScore(finalUuid)) {
                            Message.send(sender, IP.PREFIX + "Successfully reset the high score of " + finalName + " in memory and the files.");
                        } else {
                            Message.send(sender, IP.PREFIX + "<red>There was an error while trying to reset " + finalName + "'s high score.");
                        }
                    }).run();
                }

                return true;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("schematic") && player != null && player.hasPermission("witp.schematic")) {
                if (args[1].equalsIgnoreCase("paste")) {
                    String name = args[2];
                    Schematic schematic = SchematicCache.getSchematic(name);
                    if (schematic == null) {
                        Message.send(sender, IP.PREFIX + "Couldn't find " + name);
                        return true;
                    }

                    schematic.paste(player.getLocation(), RotationAngle.ANGLE_0);
                    Message.send(sender, IP.PREFIX + "Pasted schematic " + name);
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
            if (sender.hasPermission("witp.join")) {
                completions.add("join");
                completions.add("leave");
            }
            if (sender.hasPermission("witp.menu")) {
                completions.add("menu");
            }
            if (sender.hasPermission("witp.gamemode")) {
                completions.add("gamemode");
            }
            if (sender.hasPermission("witp.leaderboard")) {
                completions.add("leaderboard");
            }
            if (sender.hasPermission("witp.schematic")) {
                completions.add("schematic");
            }
            if (sender.hasPermission("witp.reload")) {
                completions.add("reload");
                completions.add("migrate");
                completions.add("reset");
            }
            if (sender.hasPermission("witp.recoverinventory")) {
                completions.add("recoverinventory");
            }
            return completions(args[0], completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("witp.reload")) {
                completions.add("everyone");
                for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
                    completions.add(pp.getName());
                }
            } else if (args[0].equalsIgnoreCase("schematic") && sender.hasPermission("witp.schematic")) {
                completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "paste"));
            } else if (args[0].equalsIgnoreCase("forcejoin") && sender.hasPermission("witp.forcejoin")) {
                if (sender.hasPermission("witp.forcejoin.everyone")) {
                    completions.add("nearest");
                    completions.add("everyone");
                }
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    completions.add(pl.getName());
                }
            } else if (args[0].equalsIgnoreCase("forceleave") && sender.hasPermission("witp.forceleave")) {
                if (sender.hasPermission("witp.forceleave.everyone")) {
                    completions.add("everyone");
                }
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    completions.add(pl.getName());
                }
            } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission("witp.recoverinventory")) {
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
        Message.send(sender, "");
        Message.send(sender, "<dark_gray><strikethrough>---------------<reset> " + IP.NAME + " <dark_gray><strikethrough>---------------<reset>");
        Message.send(sender, "");
        Message.send(sender, "<gray>/parkour <dark_gray>- Main command");
        if (sender.hasPermission("witp.join")) {
            Message.send(sender, "<gray>/parkour join [mode] <dark_gray>- Join the default gamemode or specify a mode.");
            Message.send(sender, "<gray>/parkour leave <dark_gray>- Leave the game on this server");
        }
        if (sender.hasPermission("witp.menu")) {
            Message.send(sender, "<gray>/parkour menu <dark_gray>- Open the customization menu");
        }
        if (sender.hasPermission("witp.gamemode")) {
            Message.send(sender, "<gray>/parkour gamemode <dark_gray>- Open the gamemode menu");
        }
        if (sender.hasPermission("witp.leaderboard")) {
            Message.send(sender, "<gray>/parkour leaderboard <dark_gray>- Open the leaderboard");
        }
        if (sender.hasPermission("witp.schematic")) {
            Message.send(sender, "<gray>/witp schematic <dark_gray>- Create a schematic");
        }
        if (sender.hasPermission("witp.reload")) {
            Message.send(sender, "<gray>/witp reload <dark_gray>- Reloads the messages-v3.yml file");
            Message.send(sender, "<gray>/witp migrate <dark_gray>- Migrate your Json files to MySQL");
        }
        if (sender.hasPermission("witp.reset")) {
            Message.send(sender, "<gray>/witp reset <everyone/player> <dark_gray>- Resets all highscores. <red>This can't be recovered!");
        }
        if (sender.hasPermission("witp.forcejoin")) {
            Message.send(sender, "<gray>/witp forcejoin <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to join");
        }
        if (sender.hasPermission("witp.forceleave")) {
            Message.send(sender, "<gray>/witp forceleave <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to leave");
        }
        if (sender.hasPermission("witp.recoverinventory")) {
            Message.send(sender, "<gray>/witp recoverinventory <player> <dark_gray>- Recover a player's saved inventory." +
                    " <red>Useful for recovering data after server crashes or errors when leaving.");
        }
        Message.send(sender, "");

    }
}