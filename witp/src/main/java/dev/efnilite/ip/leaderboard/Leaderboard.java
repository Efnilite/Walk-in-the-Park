package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
import dev.efnilite.vilib.util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

        IP.getStorage().init(mode);

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
        run(() -> IP.getStorage().writeScores(mode, scores), async);
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        run(() -> {
            scores.clear();
            scores.putAll(IP.getStorage().readScores(mode));

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
                .sorted((one, two) -> two.getValue().score() - one.getValue().score())
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
        scores.keySet().forEach(this::remove);
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