package dev.efnilite.witp.hook;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "witp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Efnilite";
    }

    @Override
    public boolean canRegister(){
        return true;
    }


    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public @NotNull String getVersion() {
        return WITP.getInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "player doesn't exist";
        }
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) {
            return "player is not registered";
        }

        switch (params) {
            case "highscore":
            case "high_score":
                return Integer.toString(pp.highScore);
            case "score":
            case "current_score":
                return Integer.toString(pp.getGenerator().score);
            case "time":
            case "current_time":
                return pp.getGenerator().time;
            case "version":
            case "ver":
                return WITP.getInstance().getDescription().getVersion();
            case "blocklead":
            case "lead":
                return Integer.toString(pp.blockLead);
            case "style":
                return pp.style;
            case "time_pref":
            case "time_preference":
                return pp.time;
            case "leader":
            case "rank_one":
            case "record_player":
                return Bukkit.getOfflinePlayer(ParkourPlayer.getAtPlace(1)).getName();
            case "leader_score":
            case "rank_one_score":
            case "record_score":
            case "record":
                return Integer.toString(ParkourPlayer.getHighScore(ParkourPlayer.getAtPlace(1)));
        }

        return null;
    }
}
