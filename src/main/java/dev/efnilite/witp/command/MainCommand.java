package dev.efnilite.witp.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.Version;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.particle.ParticleData;
import dev.efnilite.witp.util.particle.Particles;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.task.Tasks;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class MainCommand extends BukkitCommand {

    public static final HashMap<Player, Selection> selections = new HashMap<>();
    private ItemStack wand;

    public MainCommand() {
        if (Version.isHigherOrEqual(Version.V1_14)) {
            wand = new ItemBuilder(
                    Material.GOLDEN_AXE, "&4&lWITP Schematic Wand")
                    .setLore("&7Left click: first position", "&7Right click: second position")
                    .setPersistentData("witp", "true")
                    .buildPersistent(WITP.getInstance());
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
            send(sender, "&7--------------- &aWITP &7---------------");
            send(sender, "&a/witp &f- &7Main command");
            send(sender, "&a/witp join [player] &f- &7Join the game on this server or make another player join");
            send(sender, "&a/witp leave &f- &7Leave the game on this server");
            send(sender, "&a/witp menu &f- &7Open the customization menu");
            send(sender, "&a/witp gamemode &f- &7Open the gamemode menu");
            send(sender, "&a/witp leaderboard &f- &7Open the leaderboard");

            // Advanced settings based per permission
            if (sender.hasPermission("witp.reload") || sender.hasPermission("witp.schematic") || sender.isOp()) {
                send(sender, "&7------------ &aAdvanced &7------------)");
            }
            if (sender.hasPermission("witp.schematic")) {
                send(sender, "&a/witp schematic &f- &7Create a schematic");
            }
            if (sender.hasPermission("witp.reload")) {
                send(sender, "&a/witp reload &f- &7Reloads the lang.yml file");
                send(sender, "&a/witp migrate &f- &7Migrate your Json files to MySQL");
                send(sender, "&a/witp reset &f- &7Resets all highscores. &cBe careful when using!");
            }
            return true;
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    if (Option.PERMISSIONS && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }

                    Tasks.time("reload");
                    send(sender, "&a&l(!) &7Reloading config files..");
                    WITP.getConfiguration().reload();
                    Option.init(false);
                    long time = Tasks.end("reload");
                    send(sender, "&a&l(!) &7Reloaded all config files in " + time + "ms!");
                    return true;
                case "reset":
                    if (Option.PERMISSIONS && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }

                    ParkourUser.resetHighScores();

                    send(sender, "&4&l(!) &7Reset all high in-memory scores.");
                    return true;
                case "migrate":
                    if (Option.PERMISSIONS && !sender.hasPermission("witp.reload")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    } else if (!Option.SQL) {
                        send(sender, "&a&l(!) &7You have disabled SQL support in the config");
                        return true;
                    }

                    Tasks.time("migrate");
                    File folder = new File(WITP.getInstance().getDataFolder() + "/players/");
                    if (!folder.exists()) {
                        folder.mkdirs();
                        return true;
                    }
                    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
                    for (File file : folder.listFiles()) {
                        FileReader reader;
                        try {
                            reader = new FileReader(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            send(sender, "&a&l(!) &cError while trying to read file, check your console");
                            return true;
                        }
                        ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
                        String name = file.getName();
                        from.uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                        from.save(true);
                    }
                    send(sender, "&a&l(!) &7Your players' data has been migrated in " + Tasks.end("migrate") + "ms!");
                    return true;
            }
            if (player == null) {
                return true;
            }
            switch (args[0]) {
                case "join": {
                    if (!player.hasPermission("witp.join") && Option.PERMISSIONS) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    ParkourUser user = ParkourUser.getUser(player);
                    if (user != null) {
                        return true;
                    }
                    try {
                        ParkourPlayer pp = ParkourPlayer.register(player);
                        ParkourGenerator generator = WITP.getVersionGenerator(pp);
                        WITP.getDivider().generate(pp, generator);
                        pp.sendTranslated("joined");
                    } catch (IOException | SQLException ex) {
                        ex.printStackTrace();
                        Verbose.error("Error while joining");
                    }
                    return true;
                }
                case "leave": {
                    ParkourUser pp = ParkourUser.getUser(player);
                    if (pp == null) {
                        return true;
                    }
                    try {
                        pp.sendTranslated("left");
                        ParkourUser.unregister(pp, true, true, true);
                    } catch (IOException | InvalidStatementException ex) {
                        ex.printStackTrace();
                        Verbose.error("Error while leaving");
                    }
                    return true;
                }
                case "menu": {
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    if (Option.OPTIONS_ENABLED && pp != null) {
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
                        return true;
                    }
                    return true;
                }
                case "leaderboard":
                    if (Option.PERMISSIONS && !player.hasPermission("witp.leaderboard")) {
                        Util.sendDefaultLang(player, "cant-do");
                        return true;
                    }
                    ParkourUser.leaderboard(ParkourUser.getUser(player), player, 1);
                    break;
                case "schematic":
                    if (Option.PERMISSIONS && !player.hasPermission("witp.schematic")) {
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
                        send(player, "&4&l(!) &7Position 1 was set to " + Util.toString(player.getLocation(), true));
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
                        send(player, "&4&l(!) &7Position 2 was set to " + Util.toString(player.getLocation(), true));
                        return true;
                    case "save":
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
                    send(player, "&c&l(!) &7" + args[1] + " is not a number! Please enter a page.");
                    return true;
                }
                ParkourUser.leaderboard(ParkourUser.getUser(player), player, page);
            } else if (args[0].equalsIgnoreCase("join") && args[1] != null) {
                if (sender.isOp()) {
                    Player join = Bukkit.getPlayer(args[1]);
                    if (join == null) {
                        Verbose.error("Player " + args[1] + " doesn't exist!");
                        return true;
                    }
                    try {
                        ParkourPlayer.register(join);
                        ParkourPlayer pp = ParkourPlayer.getPlayer(join);
                        if (pp != null) {
                            pp.sendTranslated("joined");
                        }
                    } catch (IOException | SQLException ex) {
                        ex.printStackTrace();
                        Verbose.error("Error while joining");
                    }
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
                                if (searchPp.getGenerator() != null) {
                                    new ParkourSpectator(user, searchPp);
                                }
                            }
                        }
                    }
                }
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
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("search")) {
                List<String> names = new ArrayList<>();
                for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
                    String name = pp.getPlayer().getName();
                    if (player.getName().equals(name)) {
                        continue;
                    }
                    names.add(name);
                }
                return names;
            } else if (args[0].equalsIgnoreCase("schematic") && player.hasPermission("witp.schematic")) {
                return Arrays.asList("wand", "pos1", "pos2", "save");
            }
        }
        List<String> suggestions = new ArrayList<>(Arrays.asList("join", "leave", "menu", "leaderboard", "gamemode"));
        if (player.hasPermission("witp.reload")) {
            suggestions.add("reload");
            suggestions.add("migrate");
        }
        if (player.hasPermission("witp.schematic")) {
            suggestions.add("schematic");
        }
        return suggestions;
    }
}
