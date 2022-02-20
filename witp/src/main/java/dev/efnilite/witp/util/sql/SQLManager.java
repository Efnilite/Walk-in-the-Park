package dev.efnilite.witp.util.sql;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.config.Option;
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
        this.database = Option.SQL_DB.get();

        try {
            Logging.info("Connecting to SQL");

            // Load driver class
            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // For newer versions
            } catch (ClassNotFoundException old) {
                Class.forName("com.mysql.jdbc.Driver"); // For older versions
            }

            // Connect
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + Option.SQL_URL.get() + ":" + Option.SQL_PORT.get() + "/" + Option.SQL_DB.get() +
                            "?allowPublicKeyRetrieval=true" +
                            "&useSSL=false" +
                            "&useUnicode=true" +
                            "&characterEncoding=utf-8" +
                            "&autoReconnect=true" +
                            "&maxReconnects=5",
                    Option.SQL_USERNAME.get(), Option.SQL_PASSWORD.get());

            init();

            Logging.info("Successfully connected");
        } catch (Throwable throwable) {
            Logging.stack("Could not connect to SQL database", "Check your SQL settings in the config", throwable);
            Bukkit.getPluginManager().disablePlugin(WITP.getInstance()); // disable plugin since data handling without db will go horribly wrong
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
            Logging.stack("Could not confirm connection to SQL database", "Please report this error to the developer", ex);
        } catch (Throwable throwable) {
            Logging.stack("Could not reconnect to SQL database", "Please restart your server and report this error", throwable);
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
            Logging.stack("Could not send query " + query, "Please report this error to the developer", ex);
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

        sendQuery("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "players` " +
                "(`uuid` CHAR(36) NOT NULL, `name` VARCHAR(20) NULL, `highscore` INT NOT NULL, " +
                "`hstime` VARCHAR(13) NULL, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        sendQuery("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "options` " +
                "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(10), " +
                "`blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " +
                "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        sendQuery("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "game-history` (`code` CHAR(9) NOT NULL, `uuid` VARCHAR(36), " +
                "`name` VARCHAR(20), `score` VARCHAR(10), `hstime` VARCHAR(13) NULL, `difficultyScore` DECIMAL, PRIMARY KEY (`code`)) ENGINE = InnoDB CHARSET = utf8;");
        sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX.get() + "players` ADD `lang` VARCHAR(5)");
        sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX.get() + "players` ADD `hsdiff` VARCHAR(3)");
        sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX.get() + "game-history` ADD `scoreDiff` VARCHAR(3)");

        // v3.0.0
        sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX.get() + "options` DROP COLUMN `time`");
        sendQuerySuppressed("ALTER TABLE `" + Option.SQL_PREFIX.get() + "options` ADD `selectedTime` INT NOT NULL");

        Logging.info("Initialized database");
    }

    /**
     * Closes the database connection
     */
    public void close() {
        try {
            connection.close();
            Logging.info("Closed connection to MySQL");
        } catch (SQLException ex) {
            Logging.stack("Error while trying to close connection to SQL database",
                    "Please try again or report this error to the developer!", ex);
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
