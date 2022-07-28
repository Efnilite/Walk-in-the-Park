package dev.efnilite.ip.leaderboard;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.VFiles;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.sql.SelectStatement;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Class for handling leaderboards per Gamemode
 *
 * @author Efnilite
 */
public class Leaderboard {

    /**
     * The folder that is used by the leaderboard storage
     */
    public static final String FOLDER = IP.getPlugin().getDataFolder() + "/leaderboards/";

    /**
     * The file that the data of this leaderboard is stored in.
     */
    private final String file;

    /**
     * The gamemode that this leaderboard belongs to
     */
    private final String gamemode;

    /**
     * A map of all scores for this gamemode
     */
    private final Map<UUID, Score> scores = new LinkedHashMap<>();

    /**
     * A map of all scores for this gamemode, used for serializing.
     */
    @Expose
    private final Map<UUID, String> serialized = new LinkedHashMap<>();

    public Leaderboard(@NotNull String gamemode) {
        this.gamemode = gamemode.toLowerCase();
        this.file = FOLDER + gamemode.toLowerCase() + ".json";

        if (Option.SQL) {
            IP.getSqlManager().sendQuery(
                """
                USE `%s`;
                """
            .formatted(Option.SQL_DB));

            IP.getSqlManager().sendQuery(
                """
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
            .formatted(getTableName()));
        } else {
            VFiles.create(file);
        }

        // read all data
        read(true);

        // write all data every 5 minutes
        Task.create(IP.getPlugin())
                .delay(5 * Time.SECONDS_PER_MINUTE * 20)
                .repeat(5 * Time.SECONDS_PER_MINUTE * 20)
                .async()
                .execute(() -> write(false))
                .run();
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard
     */
    public void write(boolean async) {
        if (Option.SQL) {
            if (async) {
                Task.create(IP.getPlugin())
                        .async()
                        .execute(this::_writeSql)
                        .run();
            } else {
                _writeSql();
            }
        } else {
            if (async) {
                Task.create(IP.getPlugin())
                        .async()
                        .execute(this::_writeFile)
                        .run();
            } else {
                _writeFile();
            }
        }
    }

    /*
     * writes all data to the mysql table
     */
    private void _writeSql() {
        // clear table
        IP.getSqlManager().sendQuery(
            """
            TRUNCATE `%s`;
            """
        .formatted(getTableName()));

        for (UUID uuid : scores.keySet()) {
            Score score = scores.get(uuid);

            if (score == null) {
                continue;
            }

            // insert all
            IP.getSqlManager().sendQuery(
                    """
                    INSERT INTO `%s`
                    (uuid, name, time, difficulty, score)
                    VALUES
                    ('%s', '%s', '%s', '%s', %d);
                    """
            .formatted(getTableName(), uuid.toString(), score.name(), score.time(), score.difficulty(), score.score()));
        }
    }

    /*
     * writes all leaderboard data to the file
     */
    private void _writeFile() {
        try (FileWriter writer = new FileWriter(file)) {
            IP.getGson().toJson(this, writer);

            writer.flush();
        } catch (IOException ex) {
            IP.logging().stack(
                    "Error while trying to write to leaderboard file " + gamemode,
                    "reload/restart your server", ex);
        }
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        if (Option.SQL) {
            if (async) {
                Task.create(IP.getPlugin())
                        .async()
                        .execute(this::_readSql)
                        .run();
            } else {
                _readSql();
            }
        } else {
            if (async) {
                Task.create(IP.getPlugin())
                        .async()
                        .execute(this::_readFile)
                        .run();
            } else {
                _readFile();
            }
        }
    }

    /*
     * read leaderboard data from table
     */
    private void _readSql() {
        try {
            SelectStatement statement = new SelectStatement(IP.getSqlManager(), getTableName())
                    .addColumns("uuid", "name", "time", "difficulty", "score"); // select all

            // fetch all data
            Map<String, List<Object>> fetched = statement.fetch();

            if (fetched == null) {
                return;
            }

            // loop over data to setup scores variable
            for (String uuid : fetched.keySet()) {
                List<Object> objects = fetched.get(uuid);

                String name = (String) objects.get(0);
                String time = (String) objects.get(1);
                String difficulty = (String) objects.get(2);
                String score = (String) objects.get(3);

                scores.put(UUID.fromString(uuid), new Score(name, time, difficulty, Integer.parseInt(score)));
            }
        } catch (SQLException ex) {
            IP.logging().stack(
                    "Error while trying to read SQL leaderboard data",
                    "restart/reload your server", ex);
        }

        sort();
    }

    /*
     * read leaderboard data from the file
     */
    private void _readFile() {
        try (FileReader reader = new FileReader(file)) {
            Leaderboard read = IP.getGson().fromJson(reader, Leaderboard.class);

            if (read != null) {
                serialized.clear();
                serialized.putAll(read.serialized);

                scores.clear();
                for (UUID uuid : serialized.keySet()) {
                    String val = serialized.get(uuid);

                    if (val == null) {
                        continue;
                    }

                    scores.put(uuid, Score.fromString(val));
                }
            }
        } catch (IOException ex) {
            IP.logging().stack(
                    "Error while trying to read leaderboard file " + gamemode,
                    "send this file to the developer", ex);
        }

        sort();
    }

    /**
     * Sorts all scores in the map
     */
    public void sort() {
        // sort map by values

        // get all entries in a list
        List<Map.Entry<UUID, Score>> toSort = new ArrayList<>(scores.entrySet());

        // sort in reverse natural order
        toSort.sort((one, two) -> two.getValue().score() - one.getValue().score());

        // compile map back together
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();
        for (Map.Entry<UUID, Score> entry : toSort) {
            sorted.put(entry.getKey(), entry.getValue());
        }

        scores.clear();
        scores.putAll(sorted);

        serialized.clear();
        for (UUID uuid : scores.keySet()) {
            Score score = scores.get(uuid);
            serialized.put(uuid, score.toString());
        }
    }

    /**
     * Registers a new score, overriding the old one
     *
     * @param   uuid
     *          The player's uuid
     *
     * @param   score
     *          The {@link Score} instance associated with a player's run
     *
     * @return the previous score, if there was one
     */
    @Nullable
    public Score put(@NotNull UUID uuid, @NotNull Score score) {
        serialized.put(uuid, score.toString());

        Score previous = scores.put(uuid, score);

        sort();

        return previous;
    }

    /**
     * Resets the score of a player by deleting it from the internal map
     *
     * @param   uuid
     *          The UUID
     *
     * @return the previous value if one was found
     */
    @Nullable
    public Score reset(@NotNull UUID uuid) {
        serialized.remove(uuid);

        return scores.remove(uuid);
    }

    /**
     * Resets all registered scores for this gamemode
     */
    public void resetAll() {
        for (UUID uuid : scores.keySet()) {
            reset(uuid);
        }
    }

    /**
     * Returns a {@link Score} associated with a UUID
     *
     * @param   uuid
     *          The UUID
     *
     * @return the highest {@link Score} instance associated with the given UUID
     */
    @Nullable
    public Score get(@NotNull UUID uuid) {
        return scores.get(uuid);
    }

    /**
     * Gets the rank of the provided UUID
     *
     * @param   uuid
     *          The UUID
     *
     * @return the
     */
    public int getRank(@NotNull UUID uuid) {
        return new ArrayList<>(scores.keySet()).indexOf(uuid) + 1;
    }

    /**
     * Gets the score at a specified rank.
     * Ranks start at 1.
     *
     * @param   rank
     *          The rank
     *
     * @return the {@link Score} instance, null if one isn't found
     */
    @Nullable
    public Score getScoreAtRank(int rank) {
        if (scores.size() < rank) {
            return null;
        }

        return new ArrayList<>(scores.values()).get(rank - 1);
    }

    /**
     * Gets all scores
     *
     * @return all scores
     */
    public Map<UUID, Score> getScores() {
        return scores;
    }

    /**
     * Returns the gamemode of this leaderboard
     * @return the gamemode of this leaderboard
     */
    @NotNull
    public String getGamemode() {
        return gamemode;
    }

    /*
     * return the table name
     */
    private String getTableName() {
        return Option.SQL_PREFIX + "leaderboard-" + gamemode;
    }
}
