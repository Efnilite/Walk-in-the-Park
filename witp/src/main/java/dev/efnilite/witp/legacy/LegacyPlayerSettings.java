package dev.efnilite.witp.legacy;

import com.google.gson.annotations.Expose;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.ParkourOption;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.profile.PlayerProfile;
import dev.efnilite.witp.player.profile.Profile;

import java.io.FileReader;

public class LegacyPlayerSettings {

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

    public static Profile migrate(String file) {
        try {
            FileReader reader = new FileReader(file);
            Profile profile = new PlayerProfile();
            LegacyPlayerSettings legacy = WITP.getGson().fromJson(reader, LegacyPlayerSettings.class);

            profile.setSetting(ParkourOption.LANGUAGE.getName(), legacy.lang);
            profile.setSetting(ParkourOption.STYLES.getName(), legacy.style);
            profile.setSetting(ParkourOption.LEADS.getName(), String.valueOf(legacy.blockLead));
            profile.setSetting(ParkourOption.TIME.getName(), String.valueOf(legacy.selectedTime));

            profile.setSetting(ParkourOption.USE_SCHEMATICS.getName(), String.valueOf(legacy.useSchematic));
            profile.setSetting(ParkourOption.SCHEMATIC_DIFFICULTY.getName(), String.valueOf(legacy.schematicDifficulty));
            profile.setSetting(ParkourOption.SPECIAL_BLOCKS.getName(), String.valueOf(legacy.useSpecialBlocks));
            profile.setSetting(ParkourOption.SCORE_DIFFICULTY.getName(), String.valueOf(legacy.useScoreDifficulty));

            profile.setSetting(ParkourOption.SHOW_SCOREBOARD.getName(), String.valueOf(legacy.showScoreboard));
            profile.setSetting(ParkourOption.SHOW_FALL_MESSAGE.getName(), String.valueOf(legacy.showFallMessage));
            profile.setSetting(ParkourOption.PARTICLES_AND_SOUND.getName(), String.valueOf(legacy.useParticlesAndSound));

            return profile;
        } catch (Throwable throwable) {
            Logging.stack("Error while migrating legacy player file of " + file + " to new profile", "Please report this error to the developer", throwable);
            return null;
        }
    }

}