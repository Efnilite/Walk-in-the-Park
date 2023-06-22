package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.mode.Mode;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;

public class HoloHook {

    /**
     * Initializes this hook.
     */
    public static void init() {
        try {
            Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
        } catch (Exception ex) {
            IP.logging().warn("##");
            IP.logging().warn("## IP only supports Holographic Displays v3.0.0 or higher!");
            IP.logging().warn("## This hook will now be disabled.");
            IP.logging().warn("##");
            return;
        }

        HolographicDisplaysAPI.get(IP.getPlugin()).registerGlobalPlaceholder("ip_leaderboard", 100, argument -> {

            if (argument == null) {
                return "?";
            }

            // {ip_leaderboard: default, score, #1}
            String[] split = argument.replace(" ", "").split(",");

            Mode mode = Registry.getMode(split[0].toLowerCase());

            if (mode == null) {
                return "?";
            }

            Leaderboard leaderboard = mode.getLeaderboard();

            String type = split[1].toLowerCase();
            String rank = split[2].replace("#", "");

            Score score = leaderboard.getScoreAtRank(Integer.parseInt(rank));

            if (score == null) {
                return "?";
            }

            return switch (type) {
                case "score" -> Integer.toString(score.score());
                case "name" -> score.name();
                case "time" -> score.time();
                case "difficulty" -> score.difficulty();
                case "difficulty_string" -> parseDifficulty(Double.parseDouble(score.difficulty()));
                default -> "?";
            };
        });
    }

    private static String parseDifficulty(double difficulty) {
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
}