package dev.efnilite.ip.storage;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.util.Colls;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL storage manager.
 *
 * @since 5.0.0
 */
public final class StorageSQL implements Storage {

    private Connection connection;

    public StorageSQL() {
        connect();
    }

    @Override
    public void init(String mode) {
        sendUpdate("""
                    CREATE TABLE IF NOT EXISTS `%s`
                    (
                        uuid       CHAR(36) NOT NULL PRIMARY KEY,
                        name       VARCHAR(16),
                        time       VARCHAR(16),
                        difficulty VARCHAR(3),
                        score      INT
                    )
                    CHARSET = utf8 ENGINE = InnoDB;
                    """
                    .formatted(getTableName(mode)));
    }

    @Override
    public void close() {
        try {
            connection.close();
            IP.logging().info("Closed connection to MySQL");
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to close connection to SQL database", ex);
        }
    }

    @Override
    public @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        try (ResultSet results = sendQuery(
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
        scores.forEach((uuid, score) -> sendUpdate(
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
        try (ResultSet results = sendQuery(
                """
                SELECT * FROM `%s` WHERE uuid = '%s';
                """
                .formatted("%soptions".formatted(Option.SQL_PREFIX), player.getUUID()))) {

            if (results == null) {
                player.setSettings(new HashMap<>());
                return;
            }

            boolean hasNext = results.next(); // move cursor

            if (!hasNext) {
                player.setSettings(new HashMap<>());
                return;
            }

            Map<String, Object> settings = Colls.thread(ParkourPlayer.PLAYER_COLUMNS).mapv((key, value) -> {
                try {
                    return results.getObject(key);
                } catch (SQLException ex) {
                    IP.logging().stack("Error while trying to read SQL data of %s, option = %s".formatted(player.getName(), key), ex);
                    return null;
                }
            }).get();

            player.setSettings(settings);
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data of %s".formatted(player.getName()), ex);
        }
    }

    @Override
    public void writePlayer(@NotNull ParkourPlayer player) {
        sendUpdate("""
                INSERT INTO `%s`
                (uuid, style, blockLead, useParticles, useSpecial, showFallMsg, showScoreboard,
                 selectedTime, collectedRewards, locale, schematicDifficulty, sound)
                VALUES ('%s', '%s', %d, %b, %b, %b, %b, %d, '%s', '%s', %f, %b)
                ON DUPLICATE KEY UPDATE style               = '%s',
                                        blockLead           = %d,
                                        useParticles        = %b,
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
                        player.particles, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.locale,
                        player.schematicDifficulty, player.sound,

                        player.style, player.blockLead,
                        player.particles, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.locale,
                        player.schematicDifficulty, player.sound));
    }

    public void connect() {
        try {
            IP.logging().info("Connecting to MySQL");

	    Class.forName("org.mariadb.jdbc.Driver");

            connection = DriverManager.getConnection("jdbc:mariadb://" + Option.SQL_URL + ":" + Option.SQL_PORT + "/" + Option.SQL_DB +
                    "?allowPublicKeyRetrieval=true" + "&useSSL=false" + "&useUnicode=true" + "&characterEncoding=utf-8" + "&autoReconnect=true" +
                    "&maxReconnects=5" + "&user=" + Option.SQL_USERNAME + "&password=" + Option.SQL_PASSWORD);

            sendUpdate("CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB));
            sendUpdate("USE `" + Option.SQL_DB + "`;");

            sendUpdate("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "options` " + "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), " + "`blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useSpecial` BOOLEAN, " + "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");

            // v3.0.0
            sendUpdateSuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` DROP COLUMN `time`;");
            sendUpdateSuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` ADD `selectedTime` INT NOT NULL;");

            // v3.1.0
            sendUpdateSuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` ADD `collectedRewards` MEDIUMTEXT;");

            // v3.6.0
            sendUpdateSuppressed("ALTER TABLE `%s` ADD `locale` VARCHAR(8);".formatted(Option.SQL_PREFIX + "options"));
            sendUpdateSuppressed("ALTER TABLE `%s` ADD `schematicDifficulty` DOUBLE;".formatted(Option.SQL_PREFIX + "options"));

            // v4.0.0
            sendUpdateSuppressed("ALTER TABLE `%s` ADD `sound` BOOLEAN;".formatted(Option.SQL_PREFIX + "options"));

            // 5.0.0
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useStructure`;".formatted(Option.SQL_PREFIX));

            IP.logging().info("Connected to MySQL");
        } catch (Exception ex) {
            IP.logging().stack("Could not connect to MySQL", "check your SQL settings in the config", ex);
            Bukkit.getPluginManager().disablePlugin(IP.getPlugin()); // disable plugin since data handling without db will go horribly wrong
        }
    }

    private void validateConnection() {
        try {
            if (!connection.isValid(10)) {
                connect();
            }
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to reconnect to MySQL", ex);
        }
    }

    // send query and get result
    private ResultSet sendQuery(String sql) {
        validateConnection();

        try {
            return connection.prepareStatement(sql).executeQuery();
        } catch (SQLException ex) {
            IP.logging().stack("Error while sending query %s".formatted(sql), ex);
            return null;
        }
    }

    // send update
    private void sendUpdate(String sql) {
        validateConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            IP.logging().stack("Error while sending query %s".formatted(sql), ex);
        }
    }

    // if query throws an error, ignore it
    private void sendUpdateSuppressed(String sql) {
        validateConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ignored) {

        }
    }
}
