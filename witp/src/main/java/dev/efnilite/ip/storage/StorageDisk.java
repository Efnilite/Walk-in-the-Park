package dev.efnilite.ip.storage;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local disk (json) storage manager.
 *
 * @since 5.0.0
 */
// todo add concurrency locks
public final class StorageDisk implements Storage {

    @Override
    public void init(String mode) {
        // nothing to see here...
    }

    @Override
    public void close() {
        // nothing to see here...
    }

    @Override
    public @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        File file = getLeaderboardFile(mode);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (FileReader reader = new FileReader(file)) {
            LeaderboardContainer read = IP.getGson().fromJson(reader, LeaderboardContainer.class);

            if (read == null) {
                return new HashMap<>();
            }

            Map<UUID, String> serialized = new LinkedHashMap<>(read.serialized);
            Map<UUID, Score> scores = new HashMap<>();

            serialized.forEach((uuid, score) -> scores.put(uuid, Score.fromString(score)));

            return scores;
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to read leaderboard file %s".formatted(mode), ex);
            return new HashMap<>();
        }
    }

    @Override
    public void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        LeaderboardContainer container = new LeaderboardContainer();
        scores.forEach((uuid, score) -> container.serialized.put(uuid, score.toString()));

        File file = getLeaderboardFile(mode);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                IP.logging().stack("Error while trying to create leaderboard file %s".formatted(mode), ex);
            }
        }

        try (FileWriter writer = new FileWriter(getLeaderboardFile(mode))) {
            IP.getGson().toJson(container, writer);
            writer.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to write to leaderboard file %s".formatted(mode), ex);
        }
    }

    private File getLeaderboardFile(String mode) {
        return IP.getInFolder("leaderboards/%s.json".formatted(mode.toLowerCase()));
    }

    private static class LeaderboardContainer {
        @Expose
        private final Map<UUID, String> serialized = new LinkedHashMap<>();
    }

    @Override
    public void readPlayer(@NotNull ParkourPlayer player) {
        if (!getPlayerFile(player).exists()) {
            player.setSettings(new HashMap<>());
            return;
        }

        try (FileReader reader = new FileReader(getPlayerFile(player))) {
            ParkourPlayer from = IP.getGson().fromJson(reader, ParkourPlayer.class);

            Map<String, Object> settings = new HashMap<>();

            settings.put("style", from.style);
            settings.put("blockLead", from.blockLead);
            settings.put("useParticles", from.particles);
            settings.put("useStructure", from.useSchematic);
            settings.put("useSpecial", from.useSpecialBlocks);
            settings.put("showFallMsg", from.showFallMessage);
            settings.put("showScoreboard", from.showScoreboard);
            settings.put("selectedTime", from.selectedTime);
            settings.put("collectedRewards", from.collectedRewards);
            settings.put("locale", from._locale);
            settings.put("schematicDifficulty", from.schematicDifficulty);
            settings.put("sound", from.sound);

            player.setSettings(settings);
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to read disk data of %s".formatted(player.getName()), ex);
        }
    }

    @Override
    public void writePlayer(@NotNull ParkourPlayer player) {
        File file = getPlayerFile(player);

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                IP.logging().stack("Error while trying to create file to write disk data of %s to file %s".formatted(player.getName(), file), ex);
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            IP.getGson().toJson(player, writer);
            writer.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to write disk data of %s to file %s".formatted(player.getName(), file), ex);
        }
    }

    private File getPlayerFile(ParkourPlayer player) {
        return IP.getInFolder("players/%s.json".formatted(player.getUUID()));
    }
}
