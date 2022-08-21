package dev.efnilite.ip.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.generator.profile.Profile;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.SingleSession;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.sql.SelectStatement;
import dev.efnilite.ip.util.sql.Statement;
import dev.efnilite.ip.util.sql.UpdertStatement;
import dev.efnilite.vilib.util.Task;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode
 * besides Spectator Mode. Please note that this is NOT the same as {@link ParkourUser} itself.
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean useScoreDifficulty;
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
     * The uuid of the player
     */
    public UUID uuid;
    protected ParkourGenerator generator;
    protected File file;

    /**
     * The instant in ms in which the player joined.
     */
    protected final long joinTime;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, @Nullable PreviousData previousData) {
        super(player, previousData);

        this.uuid = player.getUniqueId();
        this.joinTime = System.currentTimeMillis();

        this.file = new File(IP.getPlugin().getDataFolder() + "/players/" + uuid.toString() + ".json");
        this.locale = Option.DEFAULT_LOCALE;
        this.lang = locale;

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);
    }

    /**
     * Forces this player's generator to match the settings of this player.
     */
    public void updateGeneratorSettings() {
        Profile profile = generator.getProfile();

        profile
                .setSetting("schematicDifficulty", schematicDifficulty.toString())
                .setSetting("blockLead", blockLead.toString())
                .setSetting("useScoreDifficulty", useScoreDifficulty.toString())
                .setSetting("useParticlesAndSound", useParticlesAndSound.toString())
                .setSetting("useSpecialBlocks", useSpecialBlocks.toString())
                .setSetting("showFallMessage", showFallMessage.toString())
                .setSetting("showScoreboard", showScoreboard.toString())
                .setSetting("useSchematic", useSchematic.toString())
                .setSetting("selectedTime", selectedTime.toString())
                .setSetting("style", style);
    }

    public void setSettings(String selectedTime, String style, String locale, String schematicDifficulty,
                            String blockLead, Boolean useParticles, Boolean useDifficulty, Boolean useStructure, Boolean useSpecial,
                            Boolean showDeathMsg, Boolean showScoreboard, String collectedRewards) {

        this.collectedRewards = new ArrayList<>();
        if (collectedRewards != null) {
            for (String s : collectedRewards.split(",")) {
                if (!s.isEmpty() && !this.collectedRewards.contains(s)) { // prevent empty strings and duplicates
                    this.collectedRewards.add(s);
                }
            }
        }

        // Adjustable defaults
        this.style = orDefault(style, Option.DEFAULT_STYLE, null);
        this.lang = orDefault(locale, Option.DEFAULT_LOCALE, null);
        this.locale = this.lang;

        this.useSpecialBlocks = orDefault(useSpecial, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SPECIAL_BLOCKS)), ParkourOption.SPECIAL_BLOCKS);
        this.showFallMessage = orDefault(showDeathMsg, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SHOW_FALL_MESSAGE)), ParkourOption.SHOW_FALL_MESSAGE);
        this.useScoreDifficulty = orDefault(useDifficulty, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCORE_DIFFICULTY)), ParkourOption.SCHEMATIC_DIFFICULTY);
        this.useSchematic = orDefault(useStructure, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.USE_SCHEMATICS)), ParkourOption.USE_SCHEMATICS);
        this.showScoreboard = orDefault(showScoreboard, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SHOW_SCOREBOARD)), ParkourOption.SHOW_SCOREBOARD);
        this.useParticlesAndSound = orDefault(useParticles, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.PARTICLES_AND_SOUND)), ParkourOption.PARTICLES_AND_SOUND);

        this.blockLead = Integer.parseInt(orDefault(blockLead, Option.OPTIONS_DEFAULTS.get(ParkourOption.LEADS), ParkourOption.LEADS));
        this.selectedTime = Integer.parseInt(orDefault(selectedTime, Option.OPTIONS_DEFAULTS.get(ParkourOption.TIME), ParkourOption.TIME));

        this.schematicDifficulty = Double.parseDouble(orDefault(schematicDifficulty, Option.OPTIONS_DEFAULTS.get(ParkourOption.SCHEMATIC_DIFFICULTY), ParkourOption.SCHEMATIC_DIFFICULTY));
    }

    private <T> T orDefault(T value, T def, @Nullable ParkourOption option) {
        // check for null default values
        if (def == null) {
            IP.logging().stack("Default value is null!", "Please see if there are any errors above. Check your items-v3.yml.");
        }

        // if option is disabled, return the default value
        // this allows users to set unchangeable settings
        if (option != null && !Option.OPTIONS_ENABLED.get(option)) {
            return def;
        }

        return value == null ? def : value;
    }

    private void resetPlayerPreferences() {
        setSettings(null, null, null, null,
                null, null, null, null, null, null,
                null, null);
    }

    /**
     * Saves the player's data to their file
     */
    public void save(boolean async) {
        Runnable runnable = () -> {
            try {
                if (Option.SQL) {
                    Statement statement = new UpdertStatement(IP.getSqlManager(), Option.SQL_PREFIX + "options")
                            .setDefault("uuid", uuid.toString())
                            .setDefault("selectedTime", selectedTime)
                            .setDefault("style", style)
                            .setDefault("blockLead", blockLead)
                            .setDefault("useParticles", useParticlesAndSound)
                            .setDefault("useDifficulty", useScoreDifficulty)
                            .setDefault("useStructure", useSchematic)
                            .setDefault("useSpecial", useSpecialBlocks)
                            .setDefault("showFallMsg", showFallMessage)
                            .setDefault("showScoreboard", showScoreboard)
                            .setDefault("collectedRewards", String.join(",", collectedRewards))
                            .setDefault("locale", locale)
                            .setDefault("schematicDifficulty", schematicDifficulty)
                            .setCondition("`uuid` = '" + uuid.toString() + "'"); // saves all options
                    statement.query();
                } else {
                    if (file == null) {
                        file = new File(IP.getPlugin().getDataFolder() + "/players/" + uuid.toString() + ".json");
                    }
                    if (!file.exists()) {
                        File folder = new File(IP.getPlugin().getDataFolder() + "/players");
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }
                        file.createNewFile();
                    }
                    FileWriter writer = new FileWriter(file);
                    IP.getGson().toJson(ParkourPlayer.this, writer);
                    writer.flush();
                    writer.close();
                }
            } catch (Throwable throwable) {
                IP.logging().stack("Error while saving data of player " + player.getName(), throwable);
            }
        };

        if (async) {
            Task.create(IP.getPlugin())
                    .async()
                    .execute(runnable)
                    .run();
        } else {
            runnable.run();
        }
    }

    /**
     * Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
     * the player to achieve this score using their settings.
     *
     * @return a number from 0 to 1 (both inclusive)
     */
    public String calculateDifficultyScore() {
        try {
            double score = 0.0;
            if (useSpecialBlocks) score += 0.3;          // sum:      0.3
            if (useScoreDifficulty) score += 0.2;       //           0.5
            if (useSchematic) {
                if (schematicDifficulty == 0.3) score += 0.1;      //    0.6
                else if (schematicDifficulty == 0.5) score += 0.3; //    0.8
                else if (schematicDifficulty == 0.7) score += 0.4; //    0.9
                else if (schematicDifficulty == 0.8) score += 0.5; //    1.0
            }
            return Double.toString(score).substring(0, 3);
        } catch (NullPointerException ex) {
            return "?";
        }
    }

    // Internal registering service
    @ApiStatus.Internal
    protected static ParkourPlayer register0(@NotNull ParkourPlayer pp) {
        UUID uuid = pp.getPlayer().getUniqueId();
        JOIN_COUNT++;

        pp.setCollides(false);

        if (!Option.SQL) {
            File data = new File(IP.getPlugin().getDataFolder() + "/players/" + uuid + ".json");
            if (data.exists()) {
                try {
                    FileReader reader = new FileReader(data);
                    ParkourPlayer from = IP.getGson().fromJson(reader, ParkourPlayer.class);

                    pp.setSettings(stringValue(from.selectedTime), from.style, from.lang,
                            stringValue(from.schematicDifficulty), stringValue(from.blockLead), from.useParticlesAndSound, from.useScoreDifficulty,
                            from.useSchematic, from.useSpecialBlocks, from.showFallMessage, from.showScoreboard, from.collectedRewards != null ? String.join(",", from.collectedRewards) : null);
                    reader.close();
                } catch (Throwable throwable) {
                    IP.logging().stack("Error while reading file of player " + pp.player.getName(), throwable);
                }
            } else {
                pp.resetPlayerPreferences();
            }

            players.put(pp.player, pp);
            pp.save(true);
        } else {
            try {
                SelectStatement options = new SelectStatement(IP.getSqlManager(), Option.SQL_PREFIX + "options")
                        .addColumns("uuid", "style", "blockLead", "useParticles", "useDifficulty", "useStructure", // counting starts from 0
                        "useSpecial", "showFallMsg", "showScoreboard", "selectedTime", "collectedRewards", "locale", "schematicDifficulty")
                        .addCondition("uuid = '" + uuid + "'");
                Map<String, List<Object>> map = options.fetch();
                List<Object> objects = map != null ? map.get(uuid.toString()) : null;
                if (objects != null) {
                    pp.setSettings((String) objects.get(8),
                            (String) objects.get(0),
                            (String) objects.get(10),
                            (String) objects.get(11),
                            (String) objects.get(1),
                            translateSqlBoolean((String) objects.get(2)),
                            translateSqlBoolean((String) objects.get(3)),
                            translateSqlBoolean((String) objects.get(4)),
                            translateSqlBoolean((String) objects.get(5)),
                            translateSqlBoolean((String) objects.get(6)),
                            translateSqlBoolean((String) objects.get(7)),
                            (String) objects.get(9));
                } else {
                    pp.resetPlayerPreferences();
                    pp.save(true);
                }
            } catch (Throwable throwable) {
                IP.logging().stack("Error while reading SQL data of player " + pp.player.getName(), throwable);
            }

            players.put(pp.player, pp);
        }
        return pp;
    }

    private static boolean translateSqlBoolean(String string) {
        return string.equals("1");
    }

    @Nullable
    private static String stringValue(Object object) {
        if (object == null) {
            return null;
        } else {
            return String.valueOf(object);
        }
    }

    /**
     * Gets a ParkourPlayer from their UUID
     *
     * @param   uuid
     *          The uuid
     *
     * @return the ParkourPlayer
     */
    public static @Nullable ParkourPlayer getPlayer(UUID uuid) {
        for (Player p : players.keySet()) {
            if (p.getUniqueId() == uuid) {
                return players.get(p);
            }
        }
        return null;
    }

    /**
     * Gets a ParkourPlayer from a regular Player
     *
     * @param   player
     *          The Bukkit Player
     *
     * @return the ParkourPlayer
     */
    public static @Nullable ParkourPlayer getPlayer(@Nullable Player player) {
        return player == null ? null : getPlayer(player.getUniqueId());
    }

    /**
     * Returns whether a player is currently active.
     *
     * @param   player
     *          The player
     *
     * @return true if the player is registered, false if not.
     */
    public static boolean isActive(@Nullable Player player) {
        return player != null && players.containsKey(player);
    }

    /**
     * Gets the player's {@link ParkourGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public @NotNull ParkourGenerator getGenerator() {
        if (generator == null) {
            setGenerator(new DefaultGenerator(SingleSession.create(this, Gamemodes.DEFAULT)));
        }
        return generator;
    }

    public void setGenerator(ParkourGenerator generator) {
        this.generator = generator;

        updateGeneratorSettings();

        generator.updatePreferences();
    }

    public void setBoard(FastBoard board) {
        this.board = board;
    }

    public long getJoinTime() {
        return joinTime;
    }
}