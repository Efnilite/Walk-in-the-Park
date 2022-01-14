package dev.efnilite.witp.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.fastboard.FastBoard;
import dev.efnilite.witp.util.sql.InsertStatement;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.sql.SelectStatement;
import dev.efnilite.witp.util.sql.UpdertStatement;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
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

    /**
     * Player data used in saving
     */
    public @Expose int highScore;
    public @Expose String highScoreTime;
    public @Expose int blockLead;
    public @Expose Boolean useDifficulty;
    public @Expose String highScoreDifficulty;
    public @Expose Boolean useParticles;
    public @Expose Boolean useSpecial;
    public @Expose Boolean showDeathMsg;
    public @Expose Boolean showScoreboard;
    public @Expose Boolean useStructure;
    public @Expose String time;
    public @Expose String style;
    public @Expose String lang;
    public @Expose String name; // for fixing null in leaderboard
    public @Expose double difficulty;

    public final Instant joinTime;

    public UUID uuid;
    private ParkourGenerator generator;
    private final File file;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player) {
        super(player);
        Logging.verbose("Init of Player " + player.getName());
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.joinTime = Instant.now();

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
        this.locale = Option.DEFAULT_LANG.get();
        this.lang = locale;
    }

    public void setDefaults(int highScore, String time, String style, String highScoreTime, String lang,
                            int blockLead, boolean useParticles, boolean useDifficulty, boolean useStructure, boolean useSpecial,
                            boolean showDeathMsg, boolean showScoreboard, String highScoreDifficulty) {
        this.highScoreTime = highScoreTime;
        this.useSpecial = useSpecial;
        this.showDeathMsg = showDeathMsg;
        this.highScore = highScore;
        this.blockLead = blockLead;
        this.style = style;
        this.useParticles = useParticles;
        this.time = time;
        this.useDifficulty = useDifficulty;
        this.useStructure = useStructure;
        this.showScoreboard = showScoreboard;
        this.locale = lang;
        this.lang = lang;
        this.highScoreDifficulty = highScoreDifficulty;

        player.setPlayerTime(getTime(time), false);
        updateScoreboard();
    }

    private void resetPlayerPreferences() {
        // General defaults
        this.highScore = 0;
        this.highScoreTime = "0.0s";
        this.highScoreDifficulty = "?";

        // Adjustable defaults
        this.style = Option.DEFAULT_STYLE.get();
        this.lang = Option.DEFAULT_LANG.get();

        this.useSpecial = Boolean.parseBoolean(getDefaultValue("special", "boolean"));
        this.showDeathMsg = Boolean.parseBoolean(getDefaultValue("death-msg", "boolean"));
        this.useDifficulty = Boolean.parseBoolean(getDefaultValue("adaptive-difficulty", "boolean"));
        this.useStructure = Boolean.parseBoolean(getDefaultValue("structure", "boolean"));
        this.showScoreboard = Boolean.parseBoolean(getDefaultValue("scoreboard", "boolean"));
        this.useParticles = Boolean.parseBoolean(getDefaultValue("particles", "boolean"));
        this.blockLead = Integer.parseInt(getDefaultValue("lead", "int"));
        this.difficulty = Double.parseDouble(getDefaultValue("schematic-difficulty", "double"));
        this.time = getDefaultValue("time", "string");

        this.locale = lang;
        player.setPlayerTime(getTime(time), false);
        updateScoreboard();
    }

    private String getDefaultValue(String option, String presumedType) {
        String def = Option.OPTIONS_DEFAULTS.get(option.toLowerCase());
        if (def == null) {
            switch (presumedType.toLowerCase()) {
                case "boolean":
                    return "true";
                case "double":
                    return "0.3";
                case "int":
                    return "4";
                case "string":
                    return "Day";
            }
        }
        return def;
    }

    public void setGenerator(ParkourGenerator generator) {
        this.generator = generator;
    }

    /**
     * Updates the scoreboard
     */
    @Override
    public void updateScoreboard() {
        if (showScoreboard == null) {
            showScoreboard = true;
        }
        if (showScoreboard && Option.SCOREBOARD.get() && board != null && generator != null) {
            String title = Util.color(Option.SCOREBOARD_TITLE.get());
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES;
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
                list.add(s.replace("%score%", Integer.toString(generator.score))
                        .replace("%time%", generator.time)
                        .replace("%highscore%", rank != null ? rank.toString() : "0")
                        .replace("%topscore%", top != null ? top.toString() : "0")
                        .replace("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            }
            title = translatePlaceholders(player, title);
            board.updateTitle(title.replace("%score%", Integer.toString(generator.score))
                    .replace("%time%", generator.time)
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
    public void setHighScore(int score, String time, String diff) {
        this.highScore = score;
        highScoreTime = time;
        if (diff.length() > 3) {
            diff = diff.substring(0, 3);
        }
        highScoreDifficulty = diff;
        if (scoreMap.get(uuid) == null) {
            scoreMap.put(uuid, new Highscore(player.getName(), highScoreTime, diff));
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

                    UpdertStatement statement = new UpdertStatement(WITP.getDatabase(), Option.SQL_PREFIX + "players")
                            .setDefault("uuid", uuid.toString()).setDefault("name", name)
                            .setDefault("highscore", highScore).setDefault("hstime", highScoreTime)
                            .setDefault("lang" , locale).setDefault("hsdiff", highScoreDifficulty)
                            .setCondition("`uuid` = '" + uuid.toString() + "'");
                    statement.query();
                    statement = new UpdertStatement(WITP.getDatabase(), Option.SQL_PREFIX + "options")
                            .setDefault("uuid", uuid.toString()).setDefault("time", time)
                            .setDefault("style", style).setDefault("blockLead", blockLead)
                            .setDefault("useParticles", useParticles).setDefault("useDifficulty", useDifficulty)
                            .setDefault("useStructure", useStructure).setDefault("useSpecial", useSpecial)
                            .setDefault("showFallMsg", showDeathMsg).setDefault("showScoreboard", showScoreboard)
                            .setCondition("`uuid` = '" + uuid.toString() + "'"); // saves all options
                    statement.query();
                } else {
                    if (!file.exists()) {
                        File folder = new File(WITP.getInstance().getDataFolder() + "/players");
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }
                        file.createNewFile();
                    }
                    FileWriter writer = new FileWriter(file);
                    gson.toJson(ParkourPlayer.this, writer);
                    writer.flush();
                    writer.close();
                }
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Logging.error("Error while trying to save the player's data..");
            }
        };
        if (async) {
            Tasks.asyncTask(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Saves the current game of the player
     */
    public void saveGame() {
        if (Option.GAMELOGS.get() && Option.SQL.get() && generator.score > 0) {
            InsertStatement statement = new InsertStatement(WITP.getDatabase(), Option.SQL_PREFIX + "game-history")
                    .setValue("code", Util.randomOID()).setValue("uuid", uuid.toString())
                    .setValue("name", player.getName()).setValue("score", generator.score)
                    .setValue("hstime", generator.time).setValue("scoreDiff", calculateDifficultyScore());
            try {
                statement.query();
            } catch (InvalidStatementException ex) {
                ex.printStackTrace();
                Logging.error("Error while saving game");
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
            if (useSpecial) score += 0.3;          // sum:      0.3
            if (useDifficulty) score += 0.2;       //           0.5
            if (useStructure) {
                if (difficulty == 0.3) score += 0.1;      //    0.6
                else if (difficulty == 0.5) score += 0.3; //    0.8
                else if (difficulty == 0.7) score += 0.4; //    0.9
                else if (difficulty == 0.8) score += 0.5; //    1.0
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
     * @param   player
     *          The player
     *
     * @throws  IOException
     *          Thrown if the reader fails or the getting fails
     */
    public static @NotNull ParkourPlayer register(@NotNull Player player) throws IOException, SQLException {
        return register(new ParkourPlayer(player));
    }

    /**
     * Registers a player
     * Doesn't use async reading because the system immediately needs the data.
     *
     * @param   pp
     *          The player
     *
     * @throws  IOException
     *          Thrown if the reader fails or the getting fails
     */
    public static ParkourPlayer register(@NotNull ParkourPlayer pp) throws IOException, SQLException {
        if (players.get(pp.player) == null) {
            UUID uuid = pp.getPlayer().getUniqueId();
            JOIN_COUNT++;
            if (!Option.SQL.get()) {
                File data = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid + ".json");
                if (data.exists()) {
                    Logging.verbose("Reading player data..");
                    FileReader reader = new FileReader(data);
                    ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);

                    pp.setDefaults(from.highScore, from.time, from.style, from.highScoreTime, from.lang, from.blockLead,
                            from.useParticles == null || from.useParticles,
                            from.useDifficulty == null || from.useDifficulty,
                            from.useStructure == null || from.useStructure,
                            from.useSpecial == null || from.useSpecial,
                            from.showDeathMsg == null || from.showDeathMsg,
                            from.showScoreboard == null || from.showScoreboard,
                            from.highScoreDifficulty);
                    pp.saveStats();
                    players.put(pp.player, pp);
                    reader.close();
                } else {
                    Logging.verbose("Setting new player data..");
                    pp.resetPlayerPreferences();
                    players.put(pp.player, pp);
                    pp.saveStats();
                }
            } else {
                SelectStatement select = new SelectStatement(WITP.getDatabase(),Option.SQL_PREFIX + "players")
                        .addColumns("`uuid`", "`name`", "`highscore`", "`hstime`", "`hsdiff`").addCondition("`uuid` = '" + uuid + "'");
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

                SelectStatement options = new SelectStatement(WITP.getDatabase(),Option.SQL_PREFIX + "options")
                        .addColumns("uuid", "time", "style", "blockLead", "useParticles", "useDifficulty", "useStructure",
                                "useSpecial", "showFallMsg", "showScoreboard").addCondition("uuid = '" + uuid + "'");
                map = options.fetch();
                objects = map != null ? map.get(uuid.toString()) : null;
                if (objects != null) {
                    pp.setDefaults(highscore, (String) objects.get(0), (String) objects.get(1), highScoreTime,
                            Option.DEFAULT_LANG.get(),
                            Integer.parseInt((String) objects.get(2)), translateSqlBoolean((String) objects.get(3)),
                            translateSqlBoolean((String) objects.get(4)), translateSqlBoolean((String) objects.get(5)),
                            translateSqlBoolean((String) objects.get(6)), translateSqlBoolean((String) objects.get(7)),
                            translateSqlBoolean((String) objects.get(8)), highScoreDifficulty);
                } else {
                    pp.resetPlayerPreferences();
                    pp.saveStats();
                }
                players.put(pp.player, pp);
            }
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
     * Gets the time from a string
     *
     * @param   time
     *          The time as a string
     *
     * @return the int value used to set the time
     */
    public int getTime(String time) {
        if (time == null) {
            Logging.error("Time is null, defaulting to daytime");
            this.time = "day";
            return 1000;
        }
        switch (time.toLowerCase()) {
            case "noon":
                return 6000;
            case "dawn":
                return 12500;
            case "night":
                return 15000;
            case "midnight":
                return 18000;
            case "day":
            default:
                return 1000;
        }
    }

    public void setBoard(FastBoard board) {
        this.board = board;
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
}

