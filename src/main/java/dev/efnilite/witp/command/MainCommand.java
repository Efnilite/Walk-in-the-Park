package dev.efnilite.witp.command;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import dev.efnilite.witp.version.VersionManager_v1_16_R3;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainCommand extends BukkitCommand {

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Util.color("&7--------------- &aWITP &7---------------"));
            player.sendMessage(Util.color("&a/witp &f- &7Main command"));
            player.sendMessage(Util.color("&a/witp join &f- &7Join the game on this server"));
            player.sendMessage(Util.color("&a/witp leave &f- &7Leave the game on this server"));
            player.sendMessage(Util.color("&a/witp customize &f- &7Open the customization menu"));
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("join")) {
                try {
                    ParkourPlayer.register(player);
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    if (pp != null) {
                        pp.send("&aYou joined the parkour");
                    }
                } catch (IOException ex) {
                    Verbose.error("Error while joining");
                    ex.printStackTrace();
                }
                return true;
            } else if (args[0].equalsIgnoreCase("leave")) {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    try {
                        pp.send("&cYou left the parkour");
                        ParkourPlayer.unregister(pp, true);
                    } catch (IOException ex) {
                        Verbose.error("Error while leaving");
                        ex.printStackTrace();
                    }
                }
            } else if (args[0].equalsIgnoreCase("customize")) {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    pp.menu();
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("leaderboard") && args[1] != null) {
                int page = Integer.parseInt(args[1]);
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) {
                    pp.scoreboard(page);
                }
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Arrays.asList("join", "generate", "customize", "leave");
    }
}
