package dev.efnilite.ip.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.Score;
import dev.efnilite.ip.util.sql.SelectStatement;
import dev.efnilite.ip.util.sql.Statement;
import dev.efnilite.ip.util.sql.UpdertStatement;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StorageSQL implements Storage {

    @Override
    public @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        SelectStatement statement = new SelectStatement(IP.getSqlManager(), getTableName(mode))
                .addColumns("uuid", "name", "time", "difficulty", "score"); // select all

        try {
            // fetch all data
            Map<String, List<Object>> fetched = statement.fetch();

            if (fetched == null) {
                return new HashMap<>();
            }

            Map<UUID, Score> scores = new HashMap<>();
            // loop over data to setup scores variable
            fetched.forEach((uuid, objects) -> {
                String name = (String) objects.get(0);
                String time = (String) objects.get(1);
                String difficulty = (String) objects.get(2);
                String score = (String) objects.get(3);
                scores.put(UUID.fromString(uuid), new Score(name, time, difficulty, Integer.parseInt(score)));
            });

            return scores;
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL leaderboard data", "restart/reload your server", ex);
            return new HashMap<>();
        }
    }

    @Override
    public void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        scores.forEach((uuid, score) -> IP.getSqlManager().sendQuery("""
                INSERT INTO `%s`
                (uuid, name, time, difficulty, score)
                VALUES
                ('%s', '%s', '%s', '%s', %d)
                ON DUPLICATE KEY UPDATE
                name = '%s', time = '%s', difficulty = '%s', score = %d;
                """.formatted(getTableName(mode), uuid.toString(), score.name(), score.time(), score.difficulty(),
                score.score(), score.name(), score.time(), score.difficulty(), score.score())));
    }

    // returns leaderboard table name
    private String getTableName(String mode) {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }

    @Override
    public void readPlayer(@NotNull ParkourPlayer player) {
        try {
            SelectStatement options = new SelectStatement(IP.getSqlManager(), Option.SQL_PREFIX + "options")
                    .addColumns("uuid", "style", "blockLead", "useParticles", "useDifficulty", "useStructure",
                            "useSpecial", "showFallMsg", "showScoreboard", "selectedTime", "collectedRewards",
                            "locale", "schematicDifficulty", "sound").addCondition("uuid = '%s'".formatted(player.getUUID()));

            Map<String, List<Object>> map = options.fetch();
            List<Object> objects = map != null ? map.get(player.getUUID().toString()) : null;
            if (objects != null) {
                player.setSettings((String) objects.get(8), (String) objects.get(0),
                        (String) objects.get(10), (String) objects.get(11), (String) objects.get(1),
                        translateSqlBoolean((String) objects.get(2)), translateSqlBoolean((String) objects.get(12)),
                        translateSqlBoolean((String) objects.get(3)), translateSqlBoolean((String) objects.get(4)),
                        translateSqlBoolean((String) objects.get(5)), translateSqlBoolean((String) objects.get(6)),
                        translateSqlBoolean((String) objects.get(7)), (String) objects.get(9));
            } else {
                player.resetPlayerPreferences();
                player.save(true);
            }
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data from %s".formatted(player.getName()), ex);
        }
    }

    private boolean translateSqlBoolean(String string) {
        return string == null || string.equals("1");
    }

    @Override
    public void writePlayer(@NotNull ParkourPlayer player) {
        Statement statement = new UpdertStatement(IP.getSqlManager(), Option.SQL_PREFIX + "options")
                .setDefault("uuid", player.getUUID())
                .setDefault("selectedTime", player.selectedTime)
                .setDefault("style", player.style)
                .setDefault("blockLead", player.blockLead)
                .setDefault("useParticles", player.particles)
                .setDefault("useDifficulty", player.useScoreDifficulty)
                .setDefault("useStructure", player.useSchematic)
                .setDefault("useSpecial", player.useSpecialBlocks)
                .setDefault("showFallMsg", player.showFallMessage)
                .setDefault("showScoreboard", player.showScoreboard)
                .setDefault("collectedRewards", String.join(",", player.collectedRewards))
                .setDefault("locale", player.getLocale())
                .setDefault("schematicDifficulty", player.schematicDifficulty)
                .setDefault("sound", player.sound)
                .setCondition("`uuid` = '%s'".formatted(player.getUUID())); // saves all options

        statement.query();
    }
}