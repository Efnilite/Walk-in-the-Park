package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.Highscore;
import dev.efnilite.ip.util.Util;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Hook for PlaceholderAPI
 */
@ApiStatus.Internal
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
        return IP.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "player doesn't exist";
        }
        ParkourUser user = ParkourUser.getUser(player);
        ParkourPlayer pp = null;
        if (user instanceof ParkourPlayer) {
            pp = (ParkourPlayer) user;
        } else if (user instanceof ParkourSpectator) {
            pp = ((ParkourSpectator) user).getClosest();
        }

        if (pp != null) {
            ParkourGenerator generator = pp.getGenerator();
            switch (params) {
                case "score":
                case "current_score":
                    return Integer.toString(generator.getScore());
                case "time":
                case "current_time":
                    return generator.getTime();
                case "blocklead":
                case "lead":
                    return Integer.toString(pp.blockLead);
                case "style":
                    return pp.style;
                case "time_pref":
                case "time_preference":
                    return Integer.toString(pp.selectedTime);
                case "scoreboard":
                    return pp.showScoreboard.toString();
                case "difficulty":
                    return Double.toString(pp.schematicDifficulty);
                case "difficulty_string":
                    return Util.parseDifficulty(pp.schematicDifficulty);
                default:
                    if (params.contains("score_until_") && generator instanceof DefaultGenerator defaultGenerator) {
                        String replaced = params.replace("score_until_", "");
                        int interval = Integer.parseInt(replaced);
                        if (interval > 0) {
                            return Integer.toString(interval - (defaultGenerator.getTotalScore() % interval)); // 100 - (5 % 100) = 95
                        } else {
                            return "0";
                        }
                    }
                    break;
            }
        }

        switch (params) {
            case "rank":
                return Integer.toString(ParkourUser.getRank(player.getUniqueId()));
            case "highscore":
            case "high_score":
                Integer value = ParkourPlayer.getHighScoreValue(player.getUniqueId());
                if (value == null) {
                    value = 0;
                }
                return Integer.toString(value);
            case "version":
            case "ver":
                return IP.getPlugin().getDescription().getVersion();
            case "leader":
            case "record_player":
                UUID recordPlayer = ParkourPlayer.getAtPlace(1);
                return recordPlayer == null ? "?" : Bukkit.getOfflinePlayer(recordPlayer).getName();
            case "leader_score":
            case "record_score":
            case "record":
                UUID uuid = ParkourPlayer.getAtPlace(1);
                if (uuid == null) {
                    return "?";
                }
                Integer score = ParkourPlayer.getHighScoreValue(uuid);
                return score == null ? "?" : Integer.toString(score);
            default:
                if (params.contains("player_rank_")) {
                    String replaced = params.replace("player_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        UUID uuidRank = ParkourPlayer.getAtPlace(rank);
                        if (uuidRank == null) {
                            return "?";
                        }
                        Highscore highscore = ParkourPlayer.getHighScore(uuidRank);
                        if (highscore == null) {
                            return "?";
                        }
                        return highscore.name == null ? "?" : highscore.name;
                    } else {
                        return "?";
                    }
                } else if (params.contains("score_rank_")) {
                    String replaced = params.replace("score_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        UUID uuidRank1 = ParkourPlayer.getAtPlace(rank);
                        if (uuidRank1 == null) {
                            return "?";
                        }
                        Integer score1 = ParkourPlayer.getHighScoreValue(uuidRank1);
                        return score1 == null ? "?" : Integer.toString(score1);
                    } else {
                        return "?";
                    }
                } else if (params.contains("time_rank_")) {
                    String replaced = params.replace("time_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        UUID uuidRank1 = ParkourPlayer.getAtPlace(rank);
                        if (uuidRank1 == null) {
                            return "?";
                        }
                        String time = ParkourPlayer.getHighScoreTime(uuidRank1);
                        return time == null ? "?" : time;
                    } else {
                        return "?";
                    }
                }
        }

        return null;
    }

    public static String translate(Player player, String string) {
        return PlaceholderAPI.setPlaceholders(player, string);
    }
}
