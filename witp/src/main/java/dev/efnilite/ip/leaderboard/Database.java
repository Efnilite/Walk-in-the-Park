package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.player.data.Score;

import java.util.Map;
import java.util.UUID;

// todo finish impl
public interface Database {

    void initialize();

    Map<UUID, Score> readAll();

    Score read(UUID uuid);

    void writeAll(Map<UUID, Score> map);

    void write(UUID uuid, Score score);

    String getGamemode();

}