package dev.efnilite.ip.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.hook.PlaceholderHook;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.util.sql.SelectStatement;
import dev.efnilite.ip.util.sql.UpdertStatement;
import dev.efnilite.vilib.util.Task;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode
 * besides Spectator Mode. Please note that this is NOT the same as {@link ParkourUser} itself.
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

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
        this.name = player.getName();
        this.joinTime = System.currentTimeMillis();

        this.file = new File(IP.getPlugin().getDataFolder() + "/players/" + uuid.toString() + ".json");
        this.locale = Option.DEFAULT_LANG.get();
        this.lang = locale;
    }

    public void setSettings(Integer highScore, Integer selectedTime, String style, String highScoreTime, String lang,
                            Integer blockLead, Boolean useParticles, Boolean useDifficulty, Boolean useStructure, Boolean useSpecial,
                            Boolean showDeathMsg, Boolean showScoreboard, String highScoreDifficulty, String collectedRewards) {

        // General defaults
        this.schematicDifficulty = 0.2; // todo add file support

        this.collectedRewards = new ArrayList<>();
        if (collectedRewards != null) {
            for (String s : collectedRewards.split(",")) {
                if (!s.isEmpty() && !this.collectedRewards.contains(s)) { // prevent empty strings and duplicates
                    this.collectedRewards.add(s);
                }
            }
        }

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

        updateScoreboard();
    }

    private <T> T orDefault(T value, T def) {
        if (def == null) {
            IP.logging().stack("Default value is null!", "Please see if there are any errors above. Check your items-v3.yml.");
        }

        return value == null ? def : value;
    }

    private void resetPlayerPreferences() {
        setSettings(null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null);
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
                IP.logging().error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = getHighScoreValue(uuid);
            UUID one = getAtPlace(1);
            Integer top = 0;
            Score highscore = null;
            if (one != null) {
                top = getHighScoreValue(one);
                highscore = topScores.get(one);
            }
            for (String s : lines) {
                s = translatePlaceholders(player, s); // add support for PAPI placeholders in scoreboard
                list.add(s.replace("%score%", Integer.toString(generator.getScore()))
                        .replace("%time%", generator.getTime())
                        .replace("%highscore%", rank != null ? rank.toString() : "0")
                        .replace("%topscore%", top != null ? top.toString() : "0")
                        .replace("%topplayer%", highscore != null && highscore.name() != null ? highscore.name() : "N/A")
                        .replace("%session%", getSessionId()));
            }
            title = translatePlaceholders(player, title);
            board.updateTitle(title.replace("%score%", Integer.toString(generator.getScore()))
                    .replace("%time%", generator.getTime())
                    .replace("%highscore%", rank != null ? rank.toString() : "0")
                    .replace("%topscore%", top != null ? top.toString() : "0")
                    .replace("%topplayer%", highscore != null && highscore.name() != null ? highscore.name() : "N/A")
                    .replace("%session%", getSessionId()));
            board.updateLines(list);
        }
    }

    private String translatePlaceholders(Player player, String string) {
        if (IP.getPlaceholderHook() == null) {
            return string;
        }
        return PlaceholderHook.translate(player, string);
    }

    /**
     * Returns a random material from the possible styles
     * @see DefaultGenerator#selectBlockData()
     *
     * @return a random material
     */
    public Material getRandomMaterial() {
        return IP.getRegistry().getTypeFromStyle(style).get(style);
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
        topScores.put(uuid, new Score(name, score, highScoreTime, diff));

        sortScores();
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
                    if (highScoreDifficulty == null) {
                        calculateDifficultyScore();
                    }
                    if (highScoreDifficulty.length() > 3) {
                        highScoreDifficulty = highScoreDifficulty.substring(0, 3);
                    }

                    UpdertStatement statement = new UpdertStatement(IP.getSqlManager(), Option.SQL_PREFIX.get() + "players")
                            .setDefault("uuid", uuid.toString()).setDefault("name", name)
                            .setDefault("highscore", highScore).setDefault("hstime", highScoreTime)
                            .setDefault("lang" , locale).setDefault("hsdiff", highScoreDifficulty)
                            .setCondition("`uuid` = '" + uuid.toString() + "'");
                    statement.query();
                    statement = new UpdertStatement(IP.getSqlManager(), Option.SQL_PREFIX.get() + "options")
                            .setDefault("uuid", uuid.toString()).setDefault("selectedTime", selectedTime)
                            .setDefault("style", style).setDefault("blockLead", blockLead)
                            .setDefault("useParticles", useParticlesAndSound).setDefault("useDifficulty", useScoreDifficulty)
                            .setDefault("useStructure", useSchematic).setDefault("useSpecial", useSpecialBlocks)
                            .setDefault("showFallMsg", showFallMessage).setDefault("showScoreboard", showScoreboard)
                            .setDefault("collectedRewards", String.join(",", collectedRewards))
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

    /**
     * Gets the high score of a player
     *
     * @param   player
     *          The player
     *
     * @return the high score of the player
     */
    public static @NotNull Integer getHighScoreValue(@NotNull UUID player) {
        return topScores.get(player).score();
    }

    public static @Nullable String getHighScoreTime(@NotNull UUID player) {
        return topScores.get(player).time();
    }

    public static @Nullable Score getHighScore(@NotNull UUID player) {
        return topScores.get(player);
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
        List<UUID> scores = new ArrayList<>(topScores.keySet());
        place--;
        if (scores.size() > place) {
            return scores.get(place);
        }
        return null;
    }

    // Internal registering service
    @ApiStatus.Internal
    protected static ParkourPlayer register0(@NotNull ParkourPlayer pp) {
        UUID uuid = pp.getPlayer().getUniqueId();
        JOIN_COUNT++;
        if (!Option.SQL.get()) {
            File data = new File(IP.getPlugin().getDataFolder() + "/players/" + uuid + ".json");
            if (data.exists()) {
                try {
                    FileReader reader = new FileReader(data);
                    ParkourPlayer from = IP.getGson().fromJson(reader, ParkourPlayer.class);

                    pp.setSettings(from.highScore, from.selectedTime, from.style, from.highScoreTime, from.lang,
                            from.blockLead, from.useParticlesAndSound, from.useScoreDifficulty, from.useSchematic,
                            from.useSpecialBlocks, from.showFallMessage, from.showScoreboard, from.highScoreDifficulty,
                            from.collectedRewards != null ? String.join(",", from.collectedRewards) : null);
                    reader.close();
                } catch (Throwable throwable) {
                    IP.logging().stack("Error while reading file of player " + pp.player.getName(), throwable);
                }
            } else {
                pp.resetPlayerPreferences();
            }

            players.put(pp.player, pp);
            pp.saveStats();
        } else {
            try {
                SelectStatement select = new SelectStatement(IP.getSqlManager(), Option.SQL_PREFIX.get() + "players")
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

                SelectStatement options = new SelectStatement(IP.getSqlManager(), Option.SQL_PREFIX.get() + "options")
                        .addColumns("uuid", "style", "blockLead", "useParticles", "useDifficulty", "useStructure", // counting starts from 0
                        "useSpecial", "showFallMsg", "showScoreboard", "selectedTime", "collectedRewards").addCondition("uuid = '" + uuid + "'");
                map = options.fetch();
                objects = map != null ? map.get(uuid.toString()) : null;
                if (objects != null) {
                    pp.setSettings(highscore, Integer.parseInt((String) objects.get(8)),
                            (String) objects.get(0), highScoreTime, Option.DEFAULT_LANG.get(),
                            Integer.parseInt((String) objects.get(1)), translateSqlBoolean((String) objects.get(2)),
                            translateSqlBoolean((String) objects.get(3)), translateSqlBoolean((String) objects.get(4)),
                            translateSqlBoolean((String) objects.get(5)), translateSqlBoolean((String) objects.get(6)),
                            translateSqlBoolean((String) objects.get(7)), highScoreDifficulty, (String) objects.get(9));
                } else {
                    pp.resetPlayerPreferences();
                    pp.saveStats();
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
            generator = IP.getVersionGenerator(this);
        }
        return generator;
    }

    public void setGenerator(ParkourGenerator generator) {
        this.generator = generator;
    }

    public void setBoard(FastBoard board) {
        this.board = board;
    }

    public long getJoinTime() {
        return joinTime;
    }
}