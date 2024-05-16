package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
import dev.efnilite.ip.storage.Storage;
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
     * The way in which items will be sorted.
     */
    public final SingleLeaderboardMenu.Sort sort;

    /**
     * A map of all scores for this mode
     */
    public final Map<UUID, Score> scores = new LinkedHashMap<>();

    public Leaderboard(@NotNull String mode, SingleLeaderboardMenu.Sort sort) {
        this.mode = mode.toLowerCase();
        this.sort = sort;

        Storage.init(mode);

        // read all data
        read(true);

        var interval = Config.CONFIG.getInt("storage-update-interval");

        // read/write all data every x seconds after x seconds to allow time for reading/writing
        Task.create(IP.getPlugin())
                .delay(interval * 20)
                .repeat(interval * 20)
                .async()
                .execute(Config.CONFIG.getBoolean("joining") ? () -> {
                    IP.log("Periodic saving of leaderboard data of %s".formatted(mode));

                    write(true);
                } : () -> {
                    IP.log("Periodic reading of leaderboard data of %s".formatted(mode));

                    read(true);
                })
                .run();
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard
     */
    public void write(boolean async) {
        IP.log("Saving leaderboard data of %s".formatted(mode));

        run(() -> Storage.writeScores(mode, scores), async);
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        IP.log("Reading leaderboard data of %s".formatted(mode));

        run(() -> {
            scores.clear();
            scores.putAll(Storage.readScores(mode));

            sort();
        }, async);
    }

    private void run(Runnable runnable, boolean async) {
        if (async) {
            Task.create(IP.getPlugin()).async().execute(runnable).run();
        } else {
            runnable.run();
        }
    }

    // sorts all scores in the map
    private void sort() {
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();

        scores.entrySet().stream()
                .sorted((one, two) -> switch (sort) {
                    case SCORE -> two.getValue().score() - one.getValue().score();
                    case TIME -> one.getValue().getTimeMillis() - two.getValue().getTimeMillis();
                    case DIFFICULTY -> (int) Math.signum(Double.parseDouble(two.getValue().difficulty()) - Double.parseDouble(one.getValue().difficulty()));
                })
                .forEachOrdered(entry -> sorted.put(entry.getKey(), entry.getValue()));

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
    public Score remove(@NotNull UUID uuid) {
        return scores.remove(uuid);
    }

    /**
     * Resets all registered scores for this mode
     */
    public void resetAll() {
        new HashSet<>(scores.keySet()).forEach(this::remove);
    }

    /**
     * @param uuid The {@link UUID} to get.
     * @return The {@link Score} associated with the player. If null, returns a {@link Score} instance with "?".
     */
    @NotNull
    public Score get(@NotNull UUID uuid) {
        return scores.getOrDefault(uuid, new Score("?", "?", "?", 0));
    }

    /**
     * @param uuid The uuid
     * @return The rank. Starts from 1. Returns 0 if no ranking is found.
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
}