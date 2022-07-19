package dev.efnilite.ip.legacy;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.data.PreviousData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LegacyLeaderboardPlayer extends ParkourPlayer {

    public @Expose Integer highScore;
    public @Expose String highScoreTime;
    public @Expose String name; // for fixing null in leaderboard
    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean useScoreDifficulty;
    public @Expose String highScoreDifficulty;
    public @Expose Boolean useParticlesAndSound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Boolean useSchematic;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String lang;
    public @Expose List<String> collectedRewards;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player)} instead
     *
     * @param player
     * @param previousData
     */
    public LegacyLeaderboardPlayer(@NotNull Player player, @Nullable PreviousData previousData) {
        super(player, previousData);
    }
}
