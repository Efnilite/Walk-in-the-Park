package dev.efnilite.witp;

import dev.efnilite.fycore.command.FyCommand;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.particle.ParticleData;
import dev.efnilite.fycore.particle.Particles;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Time;
import dev.efnilite.fycore.util.Version;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.player.data.InventoryData;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.PersistentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class WITPCommand extends FyCommand {

    public static final HashMap<Player, Selection> selections = new HashMap<>();
    private ItemStack wand;

    public WITPCommand() {
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
            // Help menu
            send(sender, "&7--------------- &4WITP &7---------------");
            send(sender, "&4/witp &8- &7Main command");
            if (sender.hasPermission("witp.join")) {
                send(sender, "&4/witp join [player] &8- &7Join the game on this server or make another player join");
                send(sender, "&4/witp leave &8- &7Leave the game on this server");
            }
            if (sender.hasPermission("witp.menu")) {
                send(sender, "&4/witp menu &8- &7Open the customization menu");
            }
            if (sender.hasPermission("witp.gamemode")) {
                send(sender, "&4/witp gamemode &8- &7Open the gamemode menu");
            }
            if (sender.hasPermission("witp.leaderboard")) {
                send(sender, "&4/witp leaderboard &8- &7Open the leaderboard");
            }

            // Advanced settings based per permission
            if (sender.hasPermission("witp.reload") || sender.hasPermission("witp.schematic") || sender.isOp()) {
                send(sender, "&7------------ &4&lAdvanced &7-------------");
            }
            if (sender.hasPermission("witp.schematic")) {
                send(sender, "&4/witp schematic &f- &7Create a schematic");
            }
            if (sender.hasPermission("witp.reload")) {
                send(sender, "&4&l/witp reload &8- &7Reloads the lang.yml file");
                send(sender, "&4&l/witp migrate &8- &7Migrate your Json files to MySQL");
                send(sender, "&4&l/witp reset &8- &7Resets all highscores. &cBe careful when using!");
            }
            if (sender.hasPermission("witp.recoverinventory")) {
                send(sender, "&4&l/witp recoverinventory <player> &8- &7Recover a player's saved inventory." +
                        " &cUseful for recovering data after server crashes or errors when leaving.");
            }
            return true;
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    if (!cooldown(sender, "reload", 2500)) {
                        return true;
                    }
                    if (Option.PERMISSIONS.get() && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(sender, "cant-do");
                        return true;
                    }

                    Time.timerStart("reload");
                    send(sender, "&4&l> &7Reloading config files..");

                    WITP.getConfiguration().reload();
                    Option.init(false);

                    send(sender, "&4&l> &7Reloaded all config files in " + Time.timerEnd("reload") + "ms!");
                    return true;
                case "reset":
                    if (!cooldown(sender, "reset", 2500)) {
                        return true;
                    }
                    if (Option.PERMISSIONS.get() && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(sender, "cant-do");
                        return true;
                    }

                    try {
                        ParkourUser.resetHighScores();
                        send(sender, "&4&l> &7Successfully reset all high scores in memory and the files.");
                    } catch (Throwable throwable) {
                        Logging.stack("Error while trying to reset the high scores!",
                                "Please try again or report this error to the developer!", throwable);
                        send(sender, "&4&l> &cThere was an error while trying to reset high scores.");
                    }

                    return true;
                case "migrate":
                    if (!cooldown(sender, "migrate", 2500)) {
                        return true;
                    }
                    if (Option.PERMISSIONS.get() && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(sender, "cant-do");
                        return true;
                    } else if (!Option.SQL.get()) {
                        send(sender, "&4&l> &7You have disabled SQL support in the config!");
                        return true;
                    }

                    Time.timerStart("migrate");
                    File folder = new File(WITP.getInstance().getDataFolder() + "/players/");
                    if (!folder.exists()) {
                        folder.mkdirs();
                        return true;
                    }
                    for (File file : folder.listFiles()) {
                        FileReader reader;
                        try {
                            reader = new FileReader(file);
                        } catch (FileNotFoundException ex) {
                            Logging.stack("Could not find file to migrate", "Please try again!", ex);
                            send(sender, "&4&l> &cCould not find that file, try again!");
                            return true;
                        }
                        ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);
                        String name = file.getName();
                        from.uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                        from.save(true);
                    }
                    send(sender, "&4&l> &7Your players' data has been migrated in " + Time.timerEnd("migrate") + "ms!");
                    return true;
            }
            if (player == null) {
                return true;
            }
            switch (args[0]) {
                case "join": {
                    if (!cooldown(sender, "join", 2500)) {
                        return true;
                    }
                    if (Option.PERMISSIONS.get() && !player.hasPermission("witp.join")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }

                    if (!Option.JOINING.get()) {
                        Logging.info("Player " + player.getName() + "tried joining, but parkour is disabled.");
                        return true;
                    }

                    ParkourUser user = ParkourUser.getUser(player);
                    if (user != null) {
                        return true;
                    }

                    try {
                        ParkourPlayer pp = ParkourPlayer.register(player, null);
                        WITP.getDivider().generate(pp);
                        pp.sendTranslated("joined");
                    } catch (Throwable throwable) {
                        Logging.stack("Error while joining player " + player.getName(),
                                "Please try again or report this error to the developer!", throwable);
                    }
                    return true;
                }
                case "leave": {
                    if (!cooldown(sender, "leave", 2500)) {
                        return true;
                    }
                    ParkourUser pp = ParkourUser.getUser(player);
                    if (pp == null) {
                        return true;
                    }
                    try {
                        pp.sendTranslated("left");
                        ParkourUser.unregister(pp, true, true, true);
                    } catch (Throwable throwable) {
                        Logging.stack("Error while unregistering player " + player.getName(),
                                "Please try again or report this error to the developer!", throwable);
                    }
                    return true;
                }
                case "menu": {
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    if (Option.OPTIONS_ENABLED.get() && pp != null) {
                        pp.getGenerator().menu();
                        return true;
                    }
                    return true;
                }
                case "gamemode":
                case "gm": {
                    ParkourUser user = ParkourUser.getUser(player);
                    if (user != null && user.alertCheckPermission("witp.gamemode")) {
                        user.gamemode();
                    }
                    return true;
                }
                case "leaderboard":
                    if (Option.PERMISSIONS.get() && !player.hasPermission("witp.leaderboard")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    ParkourUser.leaderboard(ParkourUser.getUser(player), player, 1);
                    break;
                case "schematic":
                    if (Option.PERMISSIONS.get() && !player.hasPermission("witp.schematic")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    send(player, "&8----------- &4&lSchematics &8-----------");
                    send(player, "");
                    send(player, "&7Welcome to the schematic creating section.");
                    send(player, "&7You can use the following commands:");
                    if (Version.isHigherOrEqual(Version.V1_14)) {
                        send(player, "&c/witp schematic wand &8- &7Get the schematic wand");
                    }
                    send(player, "&c/witp schematic pos1 &8- &7Set the first position of your selection");
                    send(player, "&c/witp schematic pos2 &8- &7Set the second position of your selection");
                    send(player, "&c/witp schematic save &8- &7Save your selection to a schematic file");
                    send(player, "");
                    send(player, "&8&nHave any questions or need help? Join the Discord!");
                    return true;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("schematic") && player != null && player.hasPermission("witp.schematic")) {
                Selection selection = selections.get(player);
                switch (args[1].toLowerCase()) {
                    case "wand":
                        if (Version.isHigherOrEqual(Version.V1_14)) {
                            player.getInventory().addItem(wand);

                            send(player, "&8----------- &4&lSchematics &8-----------");
                            send(player, "&7Use your WITP Schematic Wand to easily select schematics.");
                            send(player, "&7Use &8left click&7 to set the first position, and &8right click &7for the second!");
                            send(player, "&7If you can't place a block and need to set a position mid-air, use &8the pos commands &7instead.");
                        }
                        return true;
                    case "pos1":
                        if (selections.get(player) == null) {
                            selections.put(player, new Selection(player.getLocation(), null, player.getWorld()));
                        } else {
                            Location pos1 = player.getLocation();
                            Location pos2 = selections.get(player).getPos2();
                            selections.put(player, new Selection(pos1, pos2, player.getWorld()));
                            Particles.box(BoundingBox.of(pos1, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        }
                        send(player, "&4&l> &7Position 1 was set to " + Util.toString(player.getLocation(), true));
                        return true;
                    case "pos2":
                        if (selections.get(player) == null) {
                            selections.put(player, new Selection(null, player.getLocation(), player.getWorld()));
                        } else {
                            Location pos1 = selections.get(player).getPos1();
                            Location pos2 = player.getLocation();
                            selections.put(player, new Selection(pos1, pos2, player.getWorld()));
                            Particles.box(BoundingBox.of(pos1, pos2), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                        }
                        send(player, "&4&l> &7Position 2 was set to " + Util.toString(player.getLocation(), true));
                        return true;
                    case "save":
                        if (!cooldown(sender, "schematic-save", 2500)) {
                            return true;
                        }
                        if (selection == null || !selection.isComplete()) {
                            send(player, "&8----------- &4&lSchematics &8-----------");
                            send(player, "&7Your schematic isn't complete yet.");
                            send(player, "&7Be sure to set the first and second position!");
                            return true;
                        }

                        String code = Util.randomDigits(6);

                        send(player, "&8----------- &4&lSchematics &8-----------");
                        send(player, "&7Your schematic is being saved..");
                        send(player, "&7Your schematic will be generated with random number code &c'" + code + "'&7!");
                        send(player, "&7You can change the file name to whatever number you like.");
                        send(player, "&8Be sure to add this schematic to &r&8schematics.yml!");

                        Schematic schematic = new Schematic(selection);
                        schematic.file("parkour-" + code).save(player);
                        return true;
                }
            } else if (args[0].equalsIgnoreCase("leaderboard") && args[1] != null && player != null) {
                int page = 0;
                try {
                    Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    send(player, "&4&l> &7" + args[1] + " is not a number! Please enter a page.");
                    return true;
                }
                ParkourUser.leaderboard(ParkourUser.getUser(player), player, page);
            } else if (args[0].equalsIgnoreCase("forcejoin") && args[1] != null && sender.hasPermission("witp.forcejoin")) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission("witp.forcejoin.everyone")) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (ParkourUser.getUser(other) != null) {
                            continue;
                        }

                        try {
                            ParkourPlayer pp = ParkourPlayer.register(other, null);
                            WITP.getDivider().generate(pp);
                            pp.sendTranslated("joined");
                        } catch (Throwable throwable) {
                            Logging.stack("Error while joining player " + other.getName(),
                                    "Please try again or report this error to the developer!", throwable);
                            send(sender, "&4&l> &cThere was an error while trying to kick everyone! Please check your console.");
                            return true;
                        }
                    }
                    send(sender, "&4&l> &7Succesfully force joined everyone");
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    send(sender, "&4&l> &7That player isn't online!");
                    return true;
                }
                try {
                    ParkourPlayer pp = ParkourPlayer.register(other, null);
                    WITP.getDivider().generate(pp);
                    pp.sendTranslated("joined");
                } catch (Throwable throwable) {
                    Logging.stack("Error while joining player " + other.getName(),
                            "Please try again or report this error to the developer!", throwable);
                }

            } else if (args[0].equalsIgnoreCase("forceleave") && args[1] != null && sender.hasPermission("witp.forceleave")) {

                if (args[1].equalsIgnoreCase("everyone") && sender.hasPermission("witp.forceleave.everyone")) {
                    for (ParkourPlayer other : ParkourUser.getActivePlayers()) {
                        try {
                            other.sendTranslated("left");
                            ParkourUser.unregister(other, true, true, true);
                        } catch (Throwable throwable) {
                            Logging.stack("Error while unregistering player " + other.getPlayer().getName(),
                                    "Please try again or report this error to the developer!", throwable);
                            send(sender, "&4&l> &cThere was an error while trying to kick everyone! Please check your console.");
                            return true;
                        }
                    }
                    send(sender, "&4&l> &7Succesfully force kicked everyone");
                    return true;
                }

                Player other = Bukkit.getPlayer(args[1]);
                if (other == null) {
                    send(sender, "&4&l> &7That player isn't online!");
                    return true;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    send(sender, "&4&lThat player isn't currently playing!");
                    return true;
                }

                try {
                    user.sendTranslated("left");
                    ParkourUser.unregister(user, true, true, true);
                } catch (Throwable throwable) {
                    Logging.stack("Error while unregistering player " + other.getName(),
                            "Please try again or report this error to the developer!", throwable);
                }

            } else if (args[0].equalsIgnoreCase("search") && player != null) {
                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    if (args[1] == null || player.getName().equalsIgnoreCase(args[1])) {
                        user.sendTranslated("not-there-search");
                    } else {
                        Player search = Bukkit.getPlayer(args[1]);
                        if (search != null && !search.getName().equals(player.getName())) {
                            ParkourUser searchUser = ParkourUser.getUser(search);
                            if (searchUser instanceof ParkourPlayer) {
                                ParkourPlayer searchPp = (ParkourPlayer) searchUser;
                                new ParkourSpectator(user, searchPp, user.getPreviousData());
                            }
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission("witp.recoverinventory")) {
                if (!cooldown(sender, "recoverinventory", 2500)) {
                    return true;
                }
                Player arg1 = Bukkit.getPlayer(args[1]);
                if (arg1 == null) {
                    send(sender, "&4&l> &7That player isn't online!");
                    return true;
                }

                InventoryData data = new InventoryData(arg1);
                data.readFile(readData -> {
                    if (readData != null) {
                        send(sender, "&4&l> &7Successfully recovered the inventory of " + arg1.getName() + " from their file");
                        if (readData.apply(true)) {
                            send(sender, "&4&l> &7Giving " + arg1.getName() + " their items now...");
                        } else {
                            send(sender, "&4&l> &cThere was an error decoding an item of " + arg1.getName());
                            send(sender, "&4&l> &7" + arg1.getName() + "'s file has been manually edited or has no saved inventory. " +
                                    "Check the console for more information.");
                        }
                    } else {
                        send(sender, "&4&l> &cThere was an error recovering the inventory of " + arg1.getName() + " from their file");
                        send(sender, "&4&l> &7" + arg1.getName() + " has no saved inventory or there was an error. Check the console.");
                    }
                });
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("askreset") && player != null && args[2] != null) {
                ParkourPlayer user = ParkourPlayer.getPlayer(player);
                if (user != null && Boolean.parseBoolean(args[2])) {
                    if (user.getGenerator() instanceof DefaultGenerator) {
                        user.send("");
                        DefaultGenerator defaultGenerator = (DefaultGenerator) user.getGenerator();
                        defaultGenerator.handler.confirmReset(args[1]);
                        defaultGenerator.reset(true);
                    }
                }
            }
        }
        return true;
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(Util.color(message));
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
            if (args[0].equalsIgnoreCase("search")) {
                for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
                    completions.add(pp.getPlayer().getName());
                }
            } else if (args[0].equalsIgnoreCase("schematic") && sender.hasPermission("witp.schematic")) {
                completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save"));
            } else if (args[0].equalsIgnoreCase("forcejoin") && sender.hasPermission("witp.forcejoin")) {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    completions.add(pl.getName());
                }
                if (sender.hasPermission("witp.forcejoin.everyone")) {
                    completions.add("everyone");
                }
            } else if (args[0].equalsIgnoreCase("forceleave") && sender.hasPermission("witp.forceleave")) {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    completions.add(pl.getName());
                }
                if (sender.hasPermission("witp.forceleave.everyone")) {
                    completions.add("everyone");
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
}
