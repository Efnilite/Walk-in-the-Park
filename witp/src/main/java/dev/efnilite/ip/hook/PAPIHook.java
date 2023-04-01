package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIHook extends PlaceholderExpansion {

    private static final Pattern INFINITE_REGEX = Pattern.compile("(.+)_(\\d+)");

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
        // placeholders that don't require a player
        switch (params) {
            case "version":
            case "ver":
                return IP.getPlugin().getDescription().getVersion();
            case "leader":
            case "record_player":
                Score score = Modes.DEFAULT.getLeaderboard().getScoreAtRank(1);

                if (score == null) {
                    return "?";
                } else {
                    return score.name();
                }
            case "leader_score":
            case "record_score":
            case "record":
                score = Modes.DEFAULT.getLeaderboard().getScoreAtRank(1);

                if (score == null) {
                    return "?";
                } else {
                    return Integer.toString(score.score());
                }
        }

        if (params.contains("player_rank_")) {
            return getInfiniteScore(params.replace("player_rank_", ""), Score::name);
        } else if (params.contains("score_rank_")) {
            return getInfiniteScore(params.replace("score_rank_", ""), Score::score);
        } else if (params.contains("time_rank_")) {
            return getInfiniteScore(params.replace("time_rank_", ""), Score::time);
        }

        // placeholders that require player
        if (player == null) {
            return "player doesn't exist";
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourPlayer pp = null;
        if (user instanceof ParkourPlayer) {
            pp = (ParkourPlayer) user;
        } else if (user instanceof ParkourSpectator) {
            pp = ((ParkourSpectator) user).closest;
        }

        if (pp != null) {
            ParkourGenerator generator = pp.generator;
            switch (params) {
                case "score", "current_score" -> {
                    return Integer.toString(generator.score);
                }
                case "time", "current_time" -> {
                    return generator.getTime();
                }
                case "blocklead", "lead" -> {
                    return Integer.toString(pp.blockLead);
                }
                case "style" -> {
                    return pp.style;
                }
                case "time_pref", "time_preference" -> {
                    return Integer.toString(pp.selectedTime);
                }
                case "scoreboard" -> {
                    return pp.showScoreboard.toString();
                }
                case "difficulty" -> {
                    return Double.toString(pp.schematicDifficulty);
                }
                case "difficulty_string" -> {
                    return parseDifficulty(pp.schematicDifficulty);
                }
                case "rank" -> {
                    return Integer.toString(Modes.DEFAULT.getLeaderboard().getRank(player.getUniqueId()));
                }
                case "highscore", "high_score" -> {
                    return Integer.toString(Modes.DEFAULT.getLeaderboard().get(player.getUniqueId()).score());
                }
                case "high_score_time" -> {
                    return Modes.DEFAULT.getLeaderboard().get(player.getUniqueId()).time();
                }
                default -> {
                    if (params.contains("score_until_")) {
                        String replaced = params.replace("score_until_", "");
                        int interval = Integer.parseInt(replaced);
                        if (interval > 0) {
                            return Integer.toString(interval - (generator.totalScore % interval)); // 100 - (5 % 100) = 95
                        } else {
                            return "0";
                        }
                    }
                }
            }
        }

        return null;
    }

    private String parseDifficulty(double difficulty) {
        if (difficulty <= 0.25) {
            return "easy";
        } else if (difficulty <= 0.5) {
            return "medium";
        } else if (difficulty <= 0.75) {
            return "hard";
        } else if (difficulty <= 1) {
            return "very hard";
        }
        return "?";
    }


    private String getInfiniteScore(String rankData, Function<Score, ?> f) {
        int rank;
        Leaderboard leaderboard;
        Matcher matcher = INFINITE_REGEX.matcher(rankData);

        // use mode-specific format
        // x_mode_rank
        if (matcher.matches()) {
            String name = matcher.group(1);
            rank = Integer.parseInt(matcher.group(2));

            Mode mode = IP.getRegistry().getMode(name);
            if (mode != null) {
                leaderboard = mode.getLeaderboard();
            } else {
                leaderboard = Modes.DEFAULT.getLeaderboard();
            }
            // use generic format
            // x_rank
        } else {
            rank = Integer.parseInt(rankData);
            leaderboard = Modes.DEFAULT.getLeaderboard();
        }

        if (rank > 0) {
            Score score = leaderboard.getScoreAtRank(rank);

            if (score == null) {
                return "?";
            } else {
                return String.valueOf(f.apply(score));
            }
        } else {
            return "?";
        }
    }
}
