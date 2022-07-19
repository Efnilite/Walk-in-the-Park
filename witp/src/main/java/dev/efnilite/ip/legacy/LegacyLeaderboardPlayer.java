package dev.efnilite.ip.legacy;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Shell class to transfer all variables
 */
public class LegacyLeaderboardPlayer {

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
}
