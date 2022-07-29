package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public boolean persist() {
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
                    return generator.getStopwatch().toString();
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

        Leaderboard leaderboard = Gamemodes.DEFAULT.getLeaderboard();

        switch (params) {
            case "rank":
                return Integer.toString(leaderboard.getRank(player.getUniqueId()));
            case "highscore":
            case "high_score":
                Score score = leaderboard.get(player.getUniqueId());

                if (score == null) {
                    return "?";
                } else {
                    return Integer.toString(score.score());
                }
            case "version":
            case "ver":
                return IP.getPlugin().getDescription().getVersion();
            case "leader":
            case "record_player":
                score = leaderboard.getScoreAtRank(1);

                if (score == null) {
                    return "?";
                } else {
                    return score.name();
                }
            case "leader_score":
            case "record_score":
            case "record":
                score = leaderboard.getScoreAtRank(1);

                if (score == null) {
                    return "?";
                } else {
                    return Integer.toString(score.score());
                }
            default:
                if (params.contains("player_rank_")) {
                    String replaced = params.replace("player_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        score = leaderboard.getScoreAtRank(rank);

                        if (score == null) {
                            return "?";
                        } else {
                            return score.name();
                        }
                    } else {
                        return "?";
                    }
                } else if (params.contains("score_rank_")) {
                    String replaced = params.replace("score_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        score = leaderboard.getScoreAtRank(rank);

                        if (score == null) {
                            return "?";
                        } else {
                            return Integer.toString(score.score());
                        }
                    } else {
                        return "?";
                    }
                } else if (params.contains("time_rank_")) {
                    String replaced = params.replace("time_rank_", "");
                    int rank = Integer.parseInt(replaced);
                    if (rank > 0) {
                        score = leaderboard.getScoreAtRank(rank);

                        if (score == null) {
                            return "?";
                        } else {
                            return score.time();
                        }
                    } else {
                        return "?";
                    }
                }
        }

        return null;
    }
}
