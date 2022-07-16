package dev.efnilite.ip.leaderboard;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.VFiles;
import dev.efnilite.vilib.util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for handling leaderboards per gamemode
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
     * A map of which users are at which rank
     */
    private final Map<UUID, Integer> ranks = new HashMap<>();

    /**
     * A map of all scores for this gamemode
     */
    @Expose
    private final Map<UUID, Score> scores = new LinkedHashMap<>();

    public Leaderboard(@NotNull String gamemode) {
        this.gamemode = gamemode;
        this.file = FOLDER + gamemode.toLowerCase() + ".json";

        VFiles.create(file);

        read();
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard
     */
    public void write() {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try (FileWriter writer = new FileWriter(file)) {
                        IP.getGson().toJson(this, writer);

                        writer.flush();
                    } catch (IOException ex) {
                        IP.logging().stack("Error while trying to write to leaderboard file " + gamemode, "reload/restart your server", ex);
                    }
                })
                .run();
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read() {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try (FileReader reader = new FileReader(file)) {
                        Leaderboard read = IP.getGson().fromJson(reader, Leaderboard.class);

                        if (read != null) {
                            this.scores.clear();
                            this.scores.putAll(read.scores);
                        }
                    } catch (IOException ex) {
                        IP.logging().stack("Error while trying to read leaderboard file " + gamemode, "send this file to the developer", ex);
                    }
                })
                .run();
    }

    /**
     * Sorts all scores in the map
     */
    public void sort() {
        LinkedHashMap<UUID, Score> sorted = scores
                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().score() - o1.getValue().score()) // reverse natural order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

        scores.clear();
        scores.putAll(sorted);
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
    public Score getAtRank(int rank) {
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
}
