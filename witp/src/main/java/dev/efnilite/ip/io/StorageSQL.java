package dev.efnilite.ip.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.Score;
import dev.efnilite.ip.util.Colls;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL storage manager.
 */
public final class StorageSQL implements Storage {

    @Override
    public @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        try (ResultSet results = IP.getSqlManager().sendQuery(
                """
                SELECT * FROM `%s`;
                """
                .formatted(getTableName(mode)))) {

            if (results == null) {
                return new HashMap<>();
            }

            Map<UUID, Score> scores = new HashMap<>();

            while (results.next()) { // advance row
                scores.put(UUID.fromString(results.getString("uuid")), new Score(
                        results.getString("name"),
                        results.getString("time"),
                        results.getString("difficulty"),
                        results.getInt("score")));
            }

            return scores;
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data of %s".formatted(mode), ex);
            return new HashMap<>();
        }
    }

    @Override
    public void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        scores.forEach((uuid, score) -> IP.getSqlManager().sendUpdate(
                """
                INSERT INTO `%s`
                    (uuid, name, time, difficulty, score)
                VALUES ('%s', '%s', '%s', '%s', %d)
                ON DUPLICATE KEY UPDATE name       = '%s',
                                        time       = '%s',
                                        difficulty = '%s',
                                        score      = %d;
                """
                .formatted(getTableName(mode), uuid.toString(), score.name(), score.time(), score.difficulty(), score.score(),
                        score.name(), score.time(), score.difficulty(), score.score())));
    }

    // returns leaderboard table name
    private String getTableName(String mode) {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }

    @Override
    public void readPlayer(@NotNull ParkourPlayer player) {
        try (ResultSet results = IP.getSqlManager().sendQuery(
                """
                SELECT * FROM `%s` WHERE uuid = '%s';
                """
                .formatted("%soptions".formatted(Option.SQL_PREFIX), player.getUUID()))) {

            if (results == null) {
                player.setSettings(new HashMap<>());
                return;
            }

            Map<String, Object> settings = Colls.mapv((key, value) -> {
                try {
                    return results.getObject(key);
                } catch (SQLException ex) {
                    IP.logging().stack("Error while trying to read SQL data of %s, option = %s".formatted(player.getName(), key), ex);
                    return null;
                }
            }, ParkourPlayer.PLAYER_COLUMNS);

            player.setSettings(settings);
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data of %s".formatted(player.getName()), ex);
        }
    }

    @Override
    public void writePlayer(@NotNull ParkourPlayer player) {
        IP.getSqlManager().sendUpdate("""
                INSERT INTO `%s`
                (uuid, style, blockLead, useParticles, useDifficulty, useStructure, useSpecial, showFallMsg, showScoreboard,
                 selectedTime, collectedRewards, locale, schematicDifficulty, sound)
                VALUES ('%s', '%s', %d, %b, %b, %b, %b, %b, %b, %d, '%s', '%s', %f, %b)
                ON DUPLICATE KEY UPDATE style               = '%s',
                                        blockLead           = %d,
                                        useParticles        = %b,
                                        useDifficulty       = %b,
                                        useStructure        = %b,
                                        useSpecial          = %b,
                                        showFallMsg         = %b,
                                        showScoreboard      = %b,
                                        selectedTime        = %d,
                                        collectedRewards    = '%s',
                                        locale              = '%s',
                                        schematicDifficulty = %f,
                                        sound               = %b;
                                        """
                .formatted("%soptions".formatted(Option.SQL_PREFIX), player.getUUID(), player.style, player.blockLead,
                        player.particles, player.useScoreDifficulty, player.useSchematic, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.getLocale(),
                        player.schematicDifficulty, player.sound,

                        player.style, player.blockLead,
                        player.particles, player.useScoreDifficulty, player.useSchematic, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.getLocale(),
                        player.schematicDifficulty, player.sound));
    }
}