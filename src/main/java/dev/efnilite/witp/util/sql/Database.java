package dev.efnilite.witp.util.sql;

import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
            Verbose.info("Connecting to SQL...");
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + url + ":" + port + "/" + database
                    + "?allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=utf-8"
                    + "&autoReconnect=true", username, password);
            init();
            Verbose.info("Connected to SQL!");
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
            Verbose.error("Error while trying to connect to SQL!");
        }
    }

    public void query(String query) {
        try {
            connection.prepareStatement(query).executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Verbose.error("Error while trying to update MySQL database!");
            Verbose.error("Query: " + query);
        }
    }

    public void suppressedQuery(String query) {
        try {
            connection.prepareStatement(query).executeUpdate();
        } catch (SQLException ex) {
            // lol
        }
    }

    public @Nullable ResultSet resultQuery(String query) {
        try {
            return connection.prepareStatement(query).executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Verbose.error("Error while trying to fetch from MySQL database!");
            Verbose.error("Query: " + query);
            return null;
        }
    }

    private void init() {
        query("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
        query("USE `" + database + "`;");

        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "players` (`uuid` CHAR(36) NOT NULL, `name` VARCHAR(20) NULL, `highscore` INT NOT NULL, " +
                "`hstime` VARCHAR(13) NULL, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "options` (`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(10)," +
                " `blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " +
                "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        query("CREATE TABLE IF NOT EXISTS `" + Option.SQL_PREFIX + "game-history` (`code` CHAR(9) NOT NULL, `uuid` VARCHAR(36), " +
                "`name` VARCHAR(20), `score` VARCHAR(10), `hstime` VARCHAR(13) NULL, `difficultyScore` DECIMAL, PRIMARY KEY (`code`)) ENGINE = InnoDB CHARSET = utf8;");
        suppressedQuery("ALTER TABLE `" + Option.SQL_PREFIX + "players` ADD `lang` VARCHAR(2)");
        Verbose.info("Initialized database");
    }

    public void close() {
        try {
            connection.close();
            Verbose.info("Closed connection to MySQL");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Verbose.error("Error while trying to close connection to MySQL database!");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}