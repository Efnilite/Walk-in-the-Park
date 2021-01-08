package dev.efnilite.witp.command;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            sender.sendMessage(Util.color("&a/witp join [player] &f- &7Join the game on this server or make another player join"));
            sender.sendMessage(Util.color("&a/witp leave &f- &7Leave the game on this server"));
            sender.sendMessage(Util.color("&a/witp menu &f- &7Open the customization menu"));
            sender.sendMessage(Util.color("&a/witp gamemode &f- &7Open the gamemode menu"));
            sender.sendMessage(Util.color("&a/witp leaderboard &f- &7Open the leaderboard"));
            return true;
        } else if (args.length == 1) {
            if (player == null) {
                return true;
            }
            if (args[0].equalsIgnoreCase("join")) {
                try {
                    ParkourPlayer.register(player);
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    if (pp != null) {
                        pp.sendTranslated("joined");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Verbose.error("Error while joining");
                }
            } else if (args[0].equalsIgnoreCase("leave")) {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    try {
                        pp.sendTranslated("left");
                        ParkourPlayer.unregister(pp, true);
                    } catch (IOException ex) {
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
                    user.gamemode();
                }
            } else if (args[0].equalsIgnoreCase("leaderboard")) {
                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    user.scoreboard(1);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("leaderboard") && args[1] != null && player != null) {
                int page = Integer.parseInt(args[1]);
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    pp.scoreboard(page);
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
                    } catch (IOException ex) {
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
        return Arrays.asList("join", "leave", "menu", "leaderboard", "gamemode");
    }
}
