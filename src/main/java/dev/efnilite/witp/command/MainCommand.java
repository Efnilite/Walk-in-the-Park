package dev.efnilite.witp.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.task.Tasks;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainCommand extends BukkitCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (args.length == 0) {
            sender.sendMessage(Util.color("&7--------------- &aWITP &7---------------"));
            sender.sendMessage(Util.color("&a/witp &f- &7Main command"));
            sender.sendMessage(Util.color("&a/witp reload &f- &7Reloads the lang.yml file"));
            sender.sendMessage(Util.color("&a/witp join [player] &f- &7Join the game on this server or make another player join"));
            sender.sendMessage(Util.color("&a/witp leave &f- &7Leave the game on this server"));
            sender.sendMessage(Util.color("&a/witp menu &f- &7Open the customization menu"));
            sender.sendMessage(Util.color("&a/witp gamemode &f- &7Open the gamemode menu"));
            sender.sendMessage(Util.color("&a/witp leaderboard &f- &7Open the leaderboard"));
            sender.sendMessage(Util.color("&a/witp migrate &f- &7Migrate your json files to MySQL"));
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("witp.reload")) {
                    Tasks.time("reload");
                    sender.sendMessage(Util.color("&a&l(!) &7Reloading config files.."));
                    WITP.getConfiguration().reload();
                    Option.init(false);
                    long time = Tasks.end("reload");
                    sender.sendMessage(Util.color("&a&l(!) &7Reloaded all config files in " + time + "ms!"));
                } else {
                    sender.sendMessage(WITP.getConfiguration().getString("lang", "messages.en.cant-do"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("migrate")) { // borrowed from ParkourUser
                if (sender.hasPermission("witp.reload")) {
                    if (Option.SQL) {
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
                                sender.sendMessage(Util.color("&a&l(!) &cError while trying to read file, check your console"));
                                return true;
                            }
                            ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
                            String name = file.getName();
                            from.uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                            from.save(true);
                        }
                        sender.sendMessage(Util.color("&a&l(!) &7Your players' data has been migrated!"));
                    } else {
                        sender.sendMessage(Util.color("&a&l(!) &7You have disabled SQL support in the config"));
                    }
                } else {
                    sender.sendMessage(WITP.getConfiguration().getString("lang", "messages.en.cant-do"));
                }
                return true;
            }
            if (player == null) {
                return true;
            }
            if (args[0].equalsIgnoreCase("join")) {
                ParkourUser pp = ParkourUser.getUser(player);
                if (pp == null) {
                    try {
                        ParkourPlayer.register(player).sendTranslated("joined");
                    } catch (IOException | SQLException ex) {
                        ex.printStackTrace();
                        Verbose.error("Error while joining");
                    }
                }
            } else if (args[0].equalsIgnoreCase("leave")) {
                ParkourUser pp = ParkourUser.getUser(player);
                if (pp != null) {
                    try {
                        pp.sendTranslated("left");
                        ParkourUser.unregister(pp, true, true, true);
                    } catch (IOException | InvalidStatementException ex) {
                        ex.printStackTrace();
                        Verbose.error("Error while leaving");
                    }
                }
            } else if (args[0].equalsIgnoreCase("menu")) {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    pp.menu();
                }
            } else if (args[0].equalsIgnoreCase("gamemode") || args[0].equalsIgnoreCase("gm")) {
                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    if (user.checkPermission("witp.gamemode")) {
                        user.gamemode();
                    }
                }
            } else if (args[0].equalsIgnoreCase("leaderboard")) {
                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    if (user.checkPermission("witp.leaderboard")) {
                        user.leaderboard(1);
                    }
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("leaderboard") && args[1] != null && player != null) {
                int page = Integer.parseInt(args[1]);
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    if (pp.checkPermission("witp.leaderboard")) {
                        pp.leaderboard(page);
                    }
                }
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
                        if (search != null) {
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
        }
        return true;
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
            }
        }
        return Arrays.asList("join", "leave", "menu", "leaderboard", "gamemode", "migrate");
    }
}
