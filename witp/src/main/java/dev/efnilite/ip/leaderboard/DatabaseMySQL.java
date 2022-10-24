package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.data.Score;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class DatabaseMySQL implements Database {


    @Override
    public void initialize() {
        IP.logging().info("Initializing MySQL data storage...");

        String database = Option.SQL_DB;

        try {
            IP.logging().info("Connecting to MySQL server...");

            // load driver class
            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // newer versions
            } catch (ClassNotFoundException old) {
                Class.forName("com.mysql.jdbc.Driver"); // older versions
            }

            // Connect
            connection = DriverManager.getConnection(
                """
                dbc:mysql://%s:%s/%s
                ?allowPublicKeyRetrieval=true
                &useSSL=false"
                &useUnicode=true
                &characterEncoding=utf-8
                &autoReconnect=true
                &maxReconnects=5
                """
            .formatted(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB), Option.SQL_USERNAME, Option.SQL_PASSWORD);

            sendQuery("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
            sendQuery("USE `" + database + "`;");

            sendQuery("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "options` " +
                    "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), " +
                    "`blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " +
                    "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");

            // v3.0.0
            sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` DROP COLUMN `time`;");
            sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` ADD `selectedTime` INT NOT NULL;");

            // v3.1.0
            sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX + "options` ADD `collectedRewards` MEDIUMTEXT;");

            // v3.6.0
            sendQuerySuppressed(
                """
                ALTER TABLE `%s` ADD `locale` VARCHAR(8);
                """
            .formatted(Option.SQL_PREFIX + "options"));

            sendQuerySuppressed(
                """
                ALTER TABLE `%s` ADD `schematicDifficulty` DOUBLE;
                """
            .formatted(Option.SQL_PREFIX + "options"));

            // v4.0.0
            sendQuerySuppressed(
                """
                ALTER TABLE `%s` ADD `sound` BOOLEAN;
                """
            .formatted(Option.SQL_PREFIX + "options"));

            IP.logging().info("Connected to MySQL server!");
        } catch (Throwable throwable) {
            IP.logging().stack("Failed to connect to MySQL database", "check your MySQL settings in the config", throwable);
            Bukkit.getPluginManager().disablePlugin(IP.getPlugin()); // disable plugin since data handling without db will go horribly wrong
        }
    }

    @Override
    public Map<UUID, Score> readAll() {
        List<UUID> uuids = new ArrayList<>();

        try {
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT uuid FROM `%s`;
                """
            .formatted(getGamemode()));

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                uuids.add(UUID.fromString(set.getString("uuid")));
            }
        } catch (SQLException ex) {

        }

        Map<UUID, Score> map = new HashMap<>();
        for (UUID uuid : uuids) {
            map.put(uuid, read(uuid));
        }

        return map;
    }

    @Override
    public Score read(UUID uuid) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT * FROM `%s`
                WHERE `uuid` = '%s';
                """
            .formatted(getGamemode(), uuid.toString()));

            ResultSet set = statement.executeQuery();

            return new Score(
                    set.getString("name"),
                    set.getString("time"),
                    set.getString("difficulty"),
                    set.getInt("score"));
        } catch (SQLException ex) {

        }

        return null;
    }

    @Override
    public void writeAll(Map<UUID, Score> map) {
        for (UUID uuid : map.keySet()) {
            write(uuid, map.get(uuid));
        }
    }

    @Override
    public void write(UUID uuid, Score score) {
        sendQuery("""
            INSERT INTO `%s`
            (uuid, name, time, difficulty, score)
            VALUES
            ('%s', '%s', '%s', '%s', %d)
            ON DUPLICATE KEY UPDATE
            name = '%s', time = '%s', difficulty = '%s', score = %d;
            """
        .formatted(getGamemode(),
                uuid.toString(), score.name(), score.time(), score.difficulty(), score.score(),
                score.name(), score.time(), score.difficulty(), score.score()));
    }

    @Override
    public String getGamemode() {
        return null;
    }

    private Connection connection;

    /**
     * Checks whether this connection is still valid or not.
     * If the connection is lost, tries to reconnect.
     */
    private void validateConnection() {
        try {
            if (!connection.isValid(10)) {
                initialize();
            }
        } catch (SQLException ex) {
            IP.logging().stack("Could not confirm connection to MySQL database", ex);
        } catch (Throwable throwable) {
            IP.logging().stack("Could not reconnect to MySQL database", throwable);
        }
    }

    /**
     * Closes the connection to the MySQL server.
     */
    private void close() {
        try {
            connection.close();
            IP.logging().info("Closed connection to MySQL server!");
        } catch (SQLException ex) {
            IP.logging().stack("Failed to close connection to MySQL database.", ex);
        }
    }

    /**
     * Sends a query to the database.
     *
     * @param   sql
     *          The query.
     */
    private void sendQuery(String sql) {
        validateConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            IP.logging().stack("Failed to execute MySQL query " + sql, ex);
        }
    }

    /**
     * Sends a query to the database. If this query returns an error, ignore it.
     *
     * @param   sql
     *          The query.
     */
    private void sendQuerySuppressed(String sql) {
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
