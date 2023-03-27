package dev.efnilite.ip.io;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.Score;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public interface Storage {

    @NotNull
    Map<UUID, Score> readScores(@NotNull String mode);

    void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores);

    void readPlayer(@NotNull ParkourPlayer player);

    void writePlayer(@NotNull ParkourPlayer player);

}
