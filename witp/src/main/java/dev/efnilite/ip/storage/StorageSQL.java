package dev.efnilite.ip.storage;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.Colls;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL storage manager.
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
                (uuid, style, blockLead, useParticles, useStructure, useSpecial, showFallMsg, showScoreboard,
                 selectedTime, collectedRewards, locale, schematicDifficulty, sound)
                VALUES ('%s', '%s', %d, %b, %b, %b, %b, %b, %d, '%s', '%s', %f, %b)
                ON DUPLICATE KEY UPDATE style               = '%s',
                                        blockLead           = %d,
                                        useParticles        = %b,
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
                        player.particles, player.useSchematic, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.locale,
                        player.schematicDifficulty, player.sound,

                        player.style, player.blockLead,
                        player.particles, player.useSchematic, player.useSpecialBlocks, player.showFallMessage,
                        player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards), player.locale,
                        player.schematicDifficulty, player.sound));
    }

    public void connect() {
        try {
            IP.logging().info("Connecting to MySQL...");

            try { // load drivers
                Class.forName("com.mysql.cj.jdbc.Driver"); // for newer versions
            } catch (ClassNotFoundException old) {
                Class.forName("com.mysql.jdbc.Driver"); // for older versions
            }

            connection = DriverManager.getConnection("jdbc:mysql://" + Option.SQL_URL + ":" + Option.SQL_PORT + "/" + Option.SQL_DB +
                    "?allowPublicKeyRetrieval=true" + "&useSSL=false" + "&useUnicode=true" + "&characterEncoding=utf-8" + "&autoReconnect=true" +
                    "&maxReconnects=5", Option.SQL_USERNAME, Option.SQL_PASSWORD);

            sendUpdate("CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB));
            sendUpdate("USE `" + Option.SQL_DB + "`;");

            sendUpdate("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "options` " + "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), " + "`blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " + "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");

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

            // v5.0.0
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(Option.SQL_PREFIX));

            IP.logging().info("Successfully connected");
        } catch (Throwable throwable) {
            IP.logging().stack("Could not connect to SQL database", "check your SQL settings in the config", throwable);
            Bukkit.getPluginManager().disablePlugin(IP.getPlugin()); // disable plugin since data handling without db will go horribly wrong
        }
    }

    /**
     * Checks whether this connection is still valid or not.
     * This utilizes an internal ping command.
     * If the connection is lost, try to reconnect.
     */
    private void validateConnection() {
        try {
            if (!connection.isValid(10)) {
                connect();
            }
        } catch (SQLException ex) {
            IP.logging().stack("Could not confirm connection to SQL database", ex);
        } catch (Exception ex) {
            IP.logging().stack("Could not reconnect to SQL database", ex);
        }
    }

    /**
     * Sends a query to the database.
     *
     * @param sql The query.
     * @return The result.
     */
    private ResultSet sendQuery(String sql) {
        validateConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeQuery();
        } catch (SQLException ex) {
            IP.logging().stack("Could not send query %s".formatted(sql), ex);
            return null;
        }
    }

    /**
     * Sends a query to the database.
     *
     * @param sql The query.
     */
    private void sendUpdate(String sql) {
        validateConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            IP.logging().stack("Could not send query %s".formatted(sql), ex);
        }
    }

    /**
     * Sends a query to the database. If this query returns an error, ignore it.
     *
     * @param sql The query.
     */
    private void sendUpdateSuppressed(String sql) {
        validateConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ignored) {
            // ignored
        }
    }
}