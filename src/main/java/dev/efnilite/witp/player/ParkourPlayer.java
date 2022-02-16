package dev.efnilite.witp.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.fycore.sql.InvalidStatementException;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Task;
import dev.efnilite.witp.ParkourOption;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.ParkourAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InsertStatement;
import dev.efnilite.witp.util.sql.SelectStatement;
import dev.efnilite.witp.util.sql.UpdertStatement;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Wrapper class for a regular player to store plugin-usable data
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

    // Player data used in saving
    public @Expose Integer highScore;
    public @Expose String highScoreTime;

    public @Expose String name; // for fixing null in leaderboard
    public @Expose Double schematicDifficulty;

    // ---------- Options ----------
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

    public final long joinTime;

    public UUID uuid;
    private ParkourGenerator generator;
    private File file;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, @Nullable PreviousData previousData) {
        super(player, previousData);
        Logging.verbose("Init of Player " + player.getName());
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.joinTime = System.currentTimeMillis();

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
        this.locale = Option.DEFAULT_LANG.get();
        this.lang = locale;
    }

    public void setSettings(Integer highScore, Integer selectedTime, String style, String highScoreTime, String lang,
                            Integer blockLead, Boolean useParticles, Boolean useDifficulty, Boolean useStructure, Boolean useSpecial,
                            Boolean showDeathMsg, Boolean showScoreboard, String highScoreDifficulty) {

        // General defaults
        this.schematicDifficulty = 0.2; // todo add file support

        this.highScore = orDefault(highScore, 0);
        this.highScoreTime = orDefault(highScoreTime, "0.0s");
        this.highScoreDifficulty = orDefault(highScoreDifficulty, "?");

        // Adjustable defaults
        this.style = orDefault(style, Option.DEFAULT_STYLE.get());
        this.lang = orDefault(lang, Option.DEFAULT_LANG.get());
        this.locale = this.lang;

        this.useSpecialBlocks = orDefault(useSpecial, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SPECIAL_BLOCKS.getName())));
        this.showFallMessage = orDefault(showDeathMsg, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SHOW_FALL_MESSAGE.getName())));
        this.useScoreDifficulty = orDefault(useDifficulty, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCORE_DIFFICULTY.getName())));
        this.useSchematic = orDefault(useStructure, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCHEMATICS.getName())));
        this.showScoreboard = orDefault(showScoreboard, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SHOW_SCOREBOARD.getName())));
        this.useParticlesAndSound = orDefault(useParticles, Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.PARTICLES_AND_SOUND.getName())));
        this.blockLead = orDefault(blockLead, Integer.parseInt(Option.OPTIONS_DEFAULTS.get(ParkourOption.LEADS.getName())));
        this.selectedTime = orDefault(selectedTime, Integer.parseInt(Option.OPTIONS_DEFAULTS.get(ParkourOption.TIME.getName())));

        updateVisualTime();
        updateScoreboard();
    }

    private <T> T orDefault(T value, T def) {
        if (def == null) {
            Logging.stack("Default value is null!", "Please see if there are any errors above. Check your items-v3.yml.");
        }

        return value == null ? def : value;
    }

    private void resetPlayerPreferences() {
        setSettings(null, null, null, null, null, null,
                null, null, null, null, null,
                null, null);
    }

    /**
     * Updates the scoreboard
     */
    @Override
    public void updateScoreboard() {
        if (showScoreboard && Option.SCOREBOARD.get() && board != null && generator != null) {
            String title = Util.color(Option.SCOREBOARD_TITLE.get());
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES; // doesn't use configoption
            if (lines == null) {
                Logging.error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = getHighScoreValue(uuid);
            UUID one = getAtPlace(1);
            Integer top = 0;
            Highscore highscore = null;
            if (one != null) {
                top = getHighScoreValue(one);
                highscore = scoreMap.get(one);
            }
            for (String s : lines) {
                s = translatePlaceholders(player, s); // add support for PAPI placeholders in scoreboard
                list.add(s.replace("%score%", Integer.toString(generator.getScore()))
                        .replace("%time%", generator.getTime())
                        .replace("%highscore%", rank != null ? rank.toString() : "0")
                        .replace("%topscore%", top != null ? top.toString() : "0")
                        .replace("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            }
            title = translatePlaceholders(player, title);
            board.updateTitle(title.replace("%score%", Integer.toString(generator.getScore()))
                    .replace("%time%", generator.getTime())
                    .replace("%highscore%", rank != null ? rank.toString() : "0")
                    .replace("%topscore%", top != null ? top.toString() : "0")
                    .replace("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            board.updateLines(list);
        }
    }

    private String translatePlaceholders(Player player, String string) {
        if (WITP.getPlaceholderHook() == null) {
            return string;
        }
        return PlaceholderHook.translate(player, string);
    }

    /**
     * Returns a random material from the possible styles
     * @see DefaultGenerator#generate()
     *
     * @return a random material
     */
    public Material randomMaterial() {
        return WITP.getRegistry().getTypeFromStyle(style).get(style);
    }

    /**
     * Sets the high score of a player
     *
     * @param   score
     *          The score
     */
    public void setHighScore(String name, int score, String time, String diff) {
        this.highScore = score;
        highScoreTime = time;
        if (diff.length() > 3) {
            diff = diff.substring(0, 3);
        }
        highScoreDifficulty = diff;
        if (scoreMap.get(uuid) == null) {
            scoreMap.put(uuid, new Highscore(name, highScoreTime, diff));
        } else {
            scoreMap.get(uuid).time = highScoreTime;
            scoreMap.get(uuid).diff = diff;
        }
        highScores.put(uuid, score);
        highScores = Util.sortByValue(highScores);
    }

    private void saveStats() {
        save(true);
    }

    /**
     * Saves the player's data to their file
     */
    public void save(boolean async) {
        Runnable runnable = () -> {
            try {
                if (Option.SQL.get()) {
                    Logging.verbose("Writing player's data to SQL server");

                    if (highScoreDifficulty == null) {
                        calculateDifficultyScore();
                    }
                    if (highScoreDifficulty.length() > 3) {
                        highScoreDifficulty = highScoreDifficulty.substring(0, 3);
                    }

                    UpdertStatement statement = new UpdertStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "players")
                            .setDefault("uuid", uuid.toString()).setDefault("name", name)
                            .setDefault("highscore", highScore).setDefault("hstime", highScoreTime)
                            .setDefault("lang" , locale).setDefault("hsdiff", highScoreDifficulty)
                            .setCondition("`uuid` = '" + uuid.toString() + "'");
                    statement.query();
                    statement = new UpdertStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "options")
                            .setDefault("uuid", uuid.toString()).setDefault("selectedTime", selectedTime)
                            .setDefault("style", style).setDefault("blockLead", blockLead)
                            .setDefault("useParticles", useParticlesAndSound).setDefault("useDifficulty", useScoreDifficulty)
                            .setDefault("useStructure", useSchematic).setDefault("useSpecial", useSpecialBlocks)
                            .setDefault("showFallMsg", showFallMessage).setDefault("showScoreboard", showScoreboard)
                            .setCondition("`uuid` = '" + uuid.toString() + "'"); // saves all options
                    statement.query();
                } else {
                    if (file == null) {
                        file = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
                    }
                    if (!file.exists()) {
                        File folder = new File(WITP.getInstance().getDataFolder() + "/players");
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }
                        file.createNewFile();
                    }
                    FileWriter writer = new FileWriter(file);
                    WITP.getGson().toJson(ParkourPlayer.this, writer);
                    writer.flush();
                    writer.close();
                }
            } catch (IOException | InvalidStatementException ex) {
                Logging.stack("Error while saving data of player " + player.getName(),
                        "Please report this error to the developer!", ex);
            }
        };
        if (async) {
            new Task()
                    .async()
                    .execute(runnable)
                    .run();
        } else {
            runnable.run();
        }
    }

    /**
     * Saves the current game of the player
     */
    public void saveGame() {
        if (Option.GAMELOGS.get() && Option.SQL.get() && generator.getScore() > 0) {
            InsertStatement statement = new InsertStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "game-history")
                    .setValue("code", Util.randomOID()).setValue("uuid", uuid.toString())
                    .setValue("name", player.getName()).setValue("score", generator.getScore())
                    .setValue("hstime", generator.getTime()).setValue("scoreDiff", calculateDifficultyScore());
            try {
                statement.query();
            } catch (InvalidStatementException throwables) {
                throwables.printStackTrace();
            }
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

    /**
     * Gets the high score of a player
     *
     * @param   player
     *          The player
     *
     * @return the high score of the player
     */
    public static @Nullable Integer getHighScoreValue(@NotNull UUID player) {
        return highScores.get(player);
    }

    public static @Nullable String getHighScoreTime(@NotNull UUID player) {
        return scoreMap.get(player).time;
    }

    public static @Nullable Highscore getHighScore(@NotNull UUID player) {
        return scoreMap.get(player);
    }


    /**
     * Gets the player at a certain place
     * Note: places are indicated in normal fashion (a.k.a. #1 is the first)
     *
     * @param   place
     *          The place
     *
     * @return the player at that place
     */
    public static @Nullable UUID getAtPlace(int place) {
        List<UUID> scores = new ArrayList<>(highScores.keySet());
        place--;
        if (scores.size() > place) {
            return scores.get(place);
        }
        return null;
    }

    /**
     * Registers a player
     * Doesn't use async reading because the system immediately needs the data.
     *
     * @param   pp
     *          The player
     */
    @ApiStatus.Internal
    protected static ParkourPlayer register0(@NotNull ParkourPlayer pp) {
        UUID uuid = pp.getPlayer().getUniqueId();
        JOIN_COUNT++;
        if (!Option.SQL.get()) {
            File data = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid + ".json");
            if (data.exists()) {
                try {
                    FileReader reader = new FileReader(data);
                    ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);

                    pp.setSettings(from.highScore, from.selectedTime, from.style, from.highScoreTime, from.lang, from.blockLead, from.useParticlesAndSound, from.useScoreDifficulty, from.useSchematic, from.useSpecialBlocks, from.showFallMessage, from.showScoreboard, from.highScoreDifficulty);
                    reader.close();
                } catch (Throwable throwable) {
                    Logging.stack("Error while reading file of player " + pp.player.getName(),
                            "Please try again or report this error to the developer!", throwable);
                }
            } else {
                Logging.verbose("Setting new player data..");
                pp.resetPlayerPreferences();
            }

            players.put(pp.player, pp);
            pp.saveStats();
        } else {
            try {
                SelectStatement select = new SelectStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "players").addColumns("`uuid`", "`name`", "`highscore`", "`hstime`", "`hsdiff`").addCondition("`uuid` = '" + uuid + "'");
                HashMap<String, List<Object>> map = select.fetch();
                List<Object> objects = map != null ? map.get(uuid.toString()) : null;
                String highScoreTime;
                String highScoreDifficulty;
                int highscore;
                if (objects != null) {
                    highscore = Integer.parseInt((String) objects.get(1));
                    highScoreTime = (String) objects.get(2);
                    highScoreDifficulty = (String) objects.get(3);
                } else {
                    pp.resetPlayerPreferences();
                    players.put(pp.player, pp);
                    pp.saveStats();
                    return pp;
                }

                SelectStatement options = new SelectStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "options").addColumns("uuid", "style", "blockLead", "useParticles", "useDifficulty", "useStructure", // counting starts from 0
                        "useSpecial", "showFallMsg", "showScoreboard", "selectedTime").addCondition("uuid = '" + uuid + "'");
                map = options.fetch();
                objects = map != null ? map.get(uuid.toString()) : null;
                if (objects != null) {
                    pp.setSettings(highscore, Integer.parseInt((String) objects.get(8)),
                            (String) objects.get(0), highScoreTime, Option.DEFAULT_LANG.get(),
                            Integer.parseInt((String) objects.get(1)), translateSqlBoolean((String) objects.get(2)),
                            translateSqlBoolean((String) objects.get(3)), translateSqlBoolean((String) objects.get(4)),
                            translateSqlBoolean((String) objects.get(5)), translateSqlBoolean((String) objects.get(6)),
                            translateSqlBoolean((String) objects.get(7)), highScoreDifficulty);
                } else {
                    pp.resetPlayerPreferences();
                    pp.saveStats();
                }
            } catch (Throwable throwable) {
                Logging.stack("Error while reading SQL data of player " + pp.player.getName(),
                        "Please try again or report this error to the developer!", throwable);
            }

            players.put(pp.player, pp);
        }
        return pp;
    }

    private static boolean translateSqlBoolean(String string) {
        return string.equals("1");
    }

    /**
     * Gets a ParkourPlayer from a regular Player
     *
     * @param   player
     *          The Bukkit Player
     * @return the ParkourPlayer
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        for (Player p : players.keySet()) {
            if (p == player) {
                return players.get(p);
            }
        }
        return null;
    }

    /**
     * Gets the player's {@link ParkourGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public @NotNull ParkourGenerator getGenerator() {
        if (generator == null) {
            generator = WITP.getVersionGenerator(this);
        }
        return generator;
    }

    public void updateVisualTime() {
        int newTime = 18000 + selectedTime;
        if (newTime >= 24000) {
            newTime -= 24000;
        }

        player.setPlayerTime(newTime, false);
    }

    public void setGenerator(ParkourGenerator generator) {
        this.generator = generator;
    }

    public void setBoard(FastBoard board) {
        this.board = board;
    }
}

