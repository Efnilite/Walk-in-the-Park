package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.io.Storage;
import dev.efnilite.ip.player.Score;
import dev.efnilite.vilib.util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class for handling leaderboards.
 */
public class Leaderboard {

    /**
     * The mode that this leaderboard belongs to
     */
    public final String mode;

    /**
     * A map of all scores for this mode
     */
    public final Map<UUID, Score> scores = new LinkedHashMap<>();

    public Leaderboard(@NotNull String mode) {
        this.mode = mode.toLowerCase();

        if (Option.SQL) {
            IP.getSqlManager().sendUpdate("""
                    CREATE TABLE IF NOT EXISTS `%s`
                    (
                        uuid       CHAR(36) NOT NULL PRIMARY KEY,
                        name       VARCHAR(16),
                        time       VARCHAR(16),
                        difficulty VARCHAR(3),
                        score      INT
                    )
                    CHARSET = utf8 ENGINE = InnoDB;
                    """.formatted(getTableName()));
        }

        // read all data
        read(true);

        // read/write all data every x seconds after x seconds to allow time for reading/writing
        Task.create(IP.getPlugin())
                .delay(Option.STORAGE_UPDATE_INTERVAL * 20)
                .repeat(Option.STORAGE_UPDATE_INTERVAL * 20)
                .async()
                .execute(Option.JOINING ? () -> write(true) : () -> read(true))
                .run();
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard
     */
    public void write(boolean async) {
        Runnable write = () -> Storage.getInstance().writeScores(mode, scores);

        if (async) {
            Task.create(IP.getPlugin()).async().execute(write).run();
        } else {
            write.run();
        }
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        Runnable read = () -> {
            scores.clear();
            scores.putAll(Storage.getInstance().readScores(mode));

            sort();
        };

        if (async) {
            Task.create(IP.getPlugin()).async().execute(read).run();
        } else {
            read.run();
        }
    }

    /**
     * Sorts all scores in the map
     */
    public void sort() {
        // get all entries in a list
        List<Map.Entry<UUID, Score>> toSort = new ArrayList<>(scores.entrySet());

        // sort in reverse natural order
        toSort.sort((one, two) -> two.getValue().score() - one.getValue().score());

        // compile map back together
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();
        toSort.forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

        scores.clear();
        scores.putAll(sorted);
    }

    /**
     * Registers a new score, overriding the old one
     *
     * @param uuid  The player's uuid
     * @param score The {@link Score} instance associated with a player's run
     * @return the previous score, if there was one
     */
    @Nullable
    public Score put(@NotNull UUID uuid, @NotNull Score score) {
        Score previous = scores.put(uuid, score);

        sort();

        return previous;
    }

    /**
     * Resets the score of a player by deleting it from the internal map
     *
     * @param uuid The UUID
     * @return the previous value if one was found
     */
    @Nullable
    public Score reset(@NotNull UUID uuid) {
        return scores.remove(uuid);
    }

    /**
     * Resets all registered scores for this mode
     */
    public void resetAll() {
        for (UUID uuid : scores.keySet()) {
            reset(uuid);
        }
    }

    /**
     * Returns a {@link Score} associated with a UUID
     *
     * @param uuid The UUID
     * @return the highest {@link Score} instance associated with the given UUID
     */
    @Nullable
    public Score get(@NotNull UUID uuid) {
        return scores.get(uuid);
    }

    /**
     * Gets the rank of the provided UUID
     *
     * @param uuid The UUID
     * @return the
     */
    public int getRank(@NotNull UUID uuid) {
        return new ArrayList<>(scores.keySet()).indexOf(uuid) + 1;
    }

    /**
     * Gets the score at a specified rank.
     * Ranks start at 1.
     *
     * @param rank The rank
     * @return the {@link Score} instance, null if one isn't found
     */
    @Nullable
    public Score getScoreAtRank(int rank) {
        if (scores.size() < rank) {
            return null;
        }

        return new ArrayList<>(scores.values()).get(rank - 1);
    }

    // return the table name
    public String getTableName() {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }
}