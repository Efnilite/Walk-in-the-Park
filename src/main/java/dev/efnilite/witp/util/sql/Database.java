package dev.efnilite.witp.util.sql;

import dev.efnilite.witp.util.Verbose;
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

        query("CREATE TABLE IF NOT EXISTS `players` (`uuid` CHAR(36) NOT NULL, `name` VARCHAR(20) NULL, `highscore` INT NOT NULL, " +
                "`hstime` VARCHAR(13) NULL, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
        query("CREATE TABLE IF NOT EXISTS `options` (`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(10)," +
                " `blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useStructure` BOOLEAN, `useSpecial` BOOLEAN, " +
                "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;");
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