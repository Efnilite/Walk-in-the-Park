package dev.efnilite.ip.io;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.Score;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public interface Storage {

    /**
     * Called when a mode leaderboard gets initiated.
     *
     * @param mode The mode.
     */
    void init(String mode);

    /**
     * Called on plugin shutdown.
     */
    void close();

    /**
     * Reads scores.
     *
     * @param mode The mode.
     * @return Map with all scores, unsorted.
     */
    @NotNull Map<UUID, Score> readScores(@NotNull String mode);

    /**
     * Writes scores.
     *
     * @param mode   The mode.
     * @param scores The score map.
     */
    void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores);

    /**
     * Reads player data and applies changes.
     *
     * @param player The player.
     */
    void readPlayer(@NotNull ParkourPlayer player);

    /**
     * Writes player data.
     *
     * @param player The player.
     */
    void writePlayer(@NotNull ParkourPlayer player);

}
