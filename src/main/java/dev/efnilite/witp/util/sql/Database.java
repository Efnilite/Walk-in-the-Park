package dev.efnilite.witp.util.sql;

import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.config.Option;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * # Options for MySQL
 **/

public class Database {

    private Connection connection;
    private String database;

    public void connect(String url, int port, String database, String username, String password) {
        this.database = database;

        try {
            Logging.info("Connecting to SQL...");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // For newer versions
            } catch (ClassNotFoundException ignored) {
                Class.forName("com.mysql.jdbc.Driver"); // For older versions
            }
            connection = DriverManager.getConnection("jdbc:mysql://" + url + ":" + port + "/" + database
                    + "?allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=utf-8"
                    + "&autoReconnect=true", username, password);
            init();
            Logging.info("Connected to SQL!");
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
            Logging.error("Error while trying to connect to SQL!");
        }
    }

    public void query(String query) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Logging.error("Error while trying to update MySQL database!");
            Logging.error("Query: " + query);
        }
    }

    public void suppressedQuery(String query) {
        try (PreparedStatement statement = connection.prepareStatement(query)){
            statement.executeUpdate();
        } catch (SQLException ex) {
            // lol
        }
    }

    public @Nullable PreparedStatement resultQuery(String query) {
        try {
            return connection.prepareStatement(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Logging.error("Error while trying to fetch from MySQL database!");
            Logging.error("Query: " + query);
            return null;
        }
    }

    private void init() {
        query("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
        query("USE `" + database + "`;");

        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "players` (`uuid` CHAR(36) NOT NULL, `name` VARCHAR(20) NULL, `highscore` INT NOT NULL, " +
                "`hstime` VARCHAR(13) NULL, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "options` (`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(10)," +
                " `blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " +
                "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX.get() + "game-history` (`code` CHAR(9) NOT NULL, `uuid` VARCHAR(36), " +
                "`name` VARCHAR(20), `score` VARCHAR(10), `hstime` VARCHAR(13) NULL, `difficultyScore` DECIMAL, PRIMARY KEY (`code`)) ENGINE = InnoDB CHARSET = utf8;");
        suppressedQuery("ALTER TABLE `" + Option.SQL_PREFIX.get() + "players` ADD `lang` VARCHAR(5)");
        suppressedQuery("ALTER TABLE `" + Option.SQL_PREFIX.get() + "players` ADD `hsdiff` VARCHAR(3)");
        suppressedQuery("ALTER TABLE `" + Option.SQL_PREFIX.get() + "game-history` ADD `scoreDiff` VARCHAR(3)");
        Logging.info("Initialized database");
    }

    public void close() {
        try {
            connection.close();
            Logging.info("Closed connection to MySQL");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Logging.error("Error while trying to close connection to MySQL database!");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
