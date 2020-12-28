package dev.efnilite.witp.command;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainCommand extends BukkitCommand {

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Util.color("&7--------------- &aWITP &7---------------"));
            player.sendMessage(Util.color("&a/witp &f- Main command"));
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
                        ParkourPlayer.unregister(pp);
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
        }
        return false;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Arrays.asList("join", "generate", "customize");
    }
}
