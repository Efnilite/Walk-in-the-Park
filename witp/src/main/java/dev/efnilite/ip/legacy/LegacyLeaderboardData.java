package dev.efnilite.ip.legacy;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.sql.SelectStatement;
import dev.efnilite.vilib.util.Time;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LegacyLeaderboardData {

    private static final Map<UUID, Score> COLLECTED = new HashMap<>();

    public static void migrate() {
        try {
            Time.timerStart("ip migrate leaderboard data");
            IP.logging().info("## ");
            IP.logging().info("## Starting migration of IP leaderboard data...");
            IP.logging().info("## ");

            IP.logging().info("## Reading old files...");

            fetchHighScores();

            IP.logging().info("## Updating leaderboard...");
            Leaderboard leaderboard = Gamemodes.DEFAULT.getLeaderboard();

            for (UUID uuid : COLLECTED.keySet()) {
                leaderboard.put(uuid, COLLECTED.get(uuid));
            }

            // save data
            IP.logging().info("## Saving new file...");
            leaderboard.write(true);
            leaderboard.sort();
            IP.logging().info("## ");
            IP.logging().info("## Finished migration of leaderboard data.");
            IP.logging().info("## Took: " + Time.timerEnd("ip migrate leaderboard data") + " ms");
            IP.logging().info("## ");
        } catch (Throwable throwable) {
            IP.logging().stack("Error while trying to migrate leaderboard data", "restart/reload your server", throwable);
        }
    }

    /**
     * Legacy method used to gather old data.
     *
     * Gets the highscores of all player
     *
     * @throws  IOException
     *          When creating the file reader goes wrong
     */
    public static void fetchHighScores() throws IOException, SQLException {
        if (Option.SQL) {
            SelectStatement per = new SelectStatement(IP.getSqlManager(), Option.SQL_PREFIX + "players")
                    .addColumns("uuid", "name", "highscore", "hstime", "hsdiff");
            HashMap<String, List<Object>> stats = per.fetch();
            if (stats != null && stats.size() > 0) {
                for (String string : stats.keySet()) {
                    List<Object> values = stats.get(string);
                    UUID uuid = UUID.fromString(string);
                    String name = (String) values.get(0);
                    int highScore = Integer.parseInt((String) values.get(1));
                    String highScoreTime = (String) values.get(2);
                    String highScoreDiff = (String) values.get(3);
                    COLLECTED.put(uuid, new Score(name, highScoreTime, highScoreDiff, highScore));
                }
            }
        } else {
            File folder = new File(IP.getPlugin().getDataFolder() + "/players/");
            if (!(folder.exists())) {
                folder.mkdirs();
                return;
            }
            for (File file : folder.listFiles()) {
                FileReader reader = new FileReader(file);
                LegacyLeaderboardPlayer from = IP.getGson().fromJson(reader, LegacyLeaderboardPlayer.class);
                if (from == null) {
                    continue;
                }
                String name = file.getName();
                UUID uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                if (from.highScoreDifficulty == null) {
                    from.highScoreDifficulty = "?";
                }

                COLLECTED.put(uuid, new Score(from.name, from.highScoreTime, from.highScoreDifficulty, from.highScore));
                reader.close();
            }
        }
    }

}