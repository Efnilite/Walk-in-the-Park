package dev.efnilite.ip.io;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.Score;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public interface Storage {

    /**
     * Reads scores.
     * @param mode The mode.
     * @return Map with all scores, unsorted.
     */
    @NotNull
    Map<UUID, Score> readScores(@NotNull String mode);

    /**
     * Writes scores.
     * @param mode The mode.
     * @param scores The score map.
     */
    void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores);

    /**
     * Reads player data and applies changes.
     * @param player The player.
     */
    void readPlayer(@NotNull ParkourPlayer player);

    /**
     * Writes player data.
     * @param player The player.
     */
    void writePlayer(@NotNull ParkourPlayer player);

    /**
     * @return The instance to use.
     */
    static Storage getInstance() {
        if (Option.SQL) {
            return new StorageSQL();
        } else {
            return new StorageDisk();
        }
    }
}
