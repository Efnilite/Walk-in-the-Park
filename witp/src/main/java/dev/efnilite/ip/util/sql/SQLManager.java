package dev.efnilite.ip.util.sql;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Main MySQL handler
 **/
public class SQLManager {

    private Connection connection;
    private String database;

    /**
     * Applies a connection to the database.
     * This method is also used to reconnect to
     */
    public void connect() {
        this.database = Option.SQL_DB;

        try {
            IP.logging().info("Connecting to SQL");

            // Load driver class
            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // For newer versions
            } catch (ClassNotFoundException old) {
                Class.forName("com.mysql.jdbc.Driver"); // For older versions
            }

            // Connect
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + Option.SQL_URL + ":" + Option.SQL_PORT + "/" + Option.SQL_DB +
                            "?allowPublicKeyRetrieval=true" +
                            "&useSSL=false" +
                            "&useUnicode=true" +
                            "&characterEncoding=utf-8" +
                            "&autoReconnect=true" +
                            "&maxReconnects=5",
                    Option.SQL_USERNAME, Option.SQL_PASSWORD);

            init();

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
    public void validateConnection() {
        try {
            if (!connection.isValid(10)) {
                connect();
            }
        } catch (SQLException ex) {
            IP.logging().stack("Could not confirm connection to SQL database", ex);
        } catch (Throwable throwable) {
            IP.logging().stack("Could not reconnect to SQL database", throwable);
        }
    }

    /**
     * Sends a query to the database.
     *
     * @param   query
     *          The query.
     */
    public void sendQuery(String query) {
        validateConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            IP.logging().stack("Could not send query " + query, ex);
        }
    }

    /**
     * Sends a query to the database. If this query returns an error, ignore it.
     *
     * @param   query
     *          The query.
     */
    public void sendQuerySuppressed(String query) {
        validateConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ignored) {
            // ignored
        }
    }

    /**
     * Initialize all tables and values.
     * This contains suppressedQueries to update outdated dbs.
     */
    private void init() {
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

        IP.logging().info("Initialized database");
    }

    /**
     * Closes the database connection
     */
    public void close() {
        try {
            connection.close();
            IP.logging().info("Closed connection to MySQL");
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to close connection to SQL database", ex);
        }
    }

    /**
     * Returns the connection
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }
}
