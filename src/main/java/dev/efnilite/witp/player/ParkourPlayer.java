package dev.efnilite.witp.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.fastboard.FastBoard;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.sql.InsertStatement;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.sql.SelectStatement;
import dev.efnilite.witp.util.sql.UpdertStatement;
import dev.efnilite.witp.util.task.Tasks;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    public @Expose boolean useDifficulty;
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
    private List<Material> possibleStyle;
    private final File file;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player) {
        super(player);
        Verbose.verbose("Init of Player " + player.getName());
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.joinTime = Instant.now();

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
        this.possibleStyle = new ArrayList<>();
        this.locale = Option.DEFAULT_LANG;
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

        setStyle(style);
        player.setPlayerTime(getTime(time), false);
        updateScoreboard();
        if (generator != null && generator instanceof DefaultGenerator) {
            ((DefaultGenerator) generator).generate(blockLead);
        }
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
        if (showScoreboard && Option.SCOREBOARD && board != null && generator != null) {
            String title = Option.SCOREBOARD_TITLE;
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES;
            if (lines == null) {
                Verbose.error("Scoreboard lines are null! Check your config!");
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
                list.add(s.replaceAll("%score%", Integer.toString(generator.score))
                        .replaceAll("%time%", generator.time)
                        .replaceAll("%highscore%", rank != null ? rank.toString() : "0")
                        .replaceAll("%topscore%", top != null ? top.toString() : "0")
                        .replaceAll("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            }
            title = translatePlaceholders(player, title);
            board.updateTitle(title.replaceAll("%score%", Integer.toString(generator.score))
                    .replaceAll("%time%", generator.time)
                    .replaceAll("%highscore%", rank != null ? rank.toString() : "0")
                    .replaceAll("%topscore%", top != null ? top.toString() : "0")
                    .replaceAll("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
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
        if (possibleStyle == null) {
            setStyle(Option.DEFAULT_STYLE);
            return randomMaterial();
        }
        return possibleStyle.get(ThreadLocalRandom.current().nextInt(possibleStyle.size()));
    }

    /**
     * Sets the style and updates the possibleStyle variable to update the Material style
     *
     * @param   style
     *          The style as listed in config.yml
     */
    public void setStyle(String style) {
        if (style == null) {
            Verbose.error("Style is null, defaulting to default style");
            style = WITP.getConfiguration().getString("config", "styles.default");
        }
        this.style = style;
        possibleStyle = getPossibleMaterials(style);
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

    /**
     * Opens the menu
     */
    public void menu() {
        InventoryBuilder builder = new InventoryBuilder(this, 3, "Customize").open();
        InventoryBuilder lead = new InventoryBuilder(this, 3, "Lead").open();
        InventoryBuilder styling = new InventoryBuilder(this, 3, "Parkour style").open();
        InventoryBuilder timeofday = new InventoryBuilder(this, 3, "Time").open();
        InventoryBuilder language = new InventoryBuilder(this, 3, "Language").open();
        Configuration config = WITP.getConfiguration();
        ItemStack close = config.getFromItemData(locale, "general.close");

        // Check which items should be displayed, out of all available (this is how DynamicInventory works, and too lazy to change it)
        // Doesn't use if/else because every value needs to be checked
        int itemCount = 9;
        if (!checkOptions("styles", "witp.style")) itemCount--;             // 1
        if (!checkOptions("lead", "witp.lead")) itemCount--;                // 2
        if (!checkOptions("time", "witp.time")) itemCount--;                // 3
        if (!checkOptions("difficulty", "witp.difficulty")) itemCount--;    // 4
        if (!checkOptions("particles", "witp.particles")) itemCount--;      // 5
        if (!checkOptions("scoreboard", "witp.scoreboard") && Option.SCOREBOARD) itemCount--; // 6
        if (!checkOptions("death-msg", "witp.fall")) itemCount--;           // 7
        if (!checkOptions("special", "witp.special")) itemCount--;          // 8
        if (!checkOptions("structure", "witp.structures")) itemCount--;     // 9


        InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(itemCount, 1);
        if (checkOptions("styles", "witp.style")) {
            builder.setItem(dynamic.next(), config.getFromItemData(locale, "options.styles", style), (t, e) -> {
                List<String> pos = Util.getNode(WITP.getConfiguration().getFile("config"), "styles.list");
                if (pos == null) {
                    Verbose.error("Error while trying to fetch possible styles from config.yml");
                    return;
                }
                int i = 0;
                for (String style : pos) {
                    if (i == 26) {
                        Verbose.error("There are too many styles to display!");
                        return;
                    }
                    List<Material> possible = this.getPossibleMaterials(style);
                    if (possible == null) {
                        continue;
                    }
                    Material material = possible.get(possible.size() - 1);
                    styling.setItem(i, new ItemBuilder(material, "&b&l" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
                        String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
                        setStyle(selected);
                        sendTranslated("selected-style", selected);
                    });
                    i++;
                }
                styling.setItem(26, close, (t2, e2) -> menu());
                styling.build();
            });
        }
        if (checkOptions("lead", "witp.lead")) {
            List<Integer> possible = Option.POSSIBLE_LEADS;
            InventoryBuilder.DynamicInventory dynamicLead = new InventoryBuilder.DynamicInventory(possible.size(), 1);
            builder.setItem(dynamic.next(), config.getFromItemData(locale, "options.lead", Integer.toString(blockLead)), (t, e) -> {
                for (Integer integer : possible) {
                    lead.setItem(dynamicLead.next(), new ItemBuilder(Material.PAPER, "&b&l" + integer).build(), (t2, e2) -> {
                        if (e2.getItemMeta() != null) {
                            blockLead = Integer.parseInt(ChatColor.stripColor(e2.getItemMeta().getDisplayName()));
                            sendTranslated("selected-block-lead", Integer.toString(blockLead));
                        }
                    });
                }
                lead.setItem(26, close, (t2, e2) -> menu());
                lead.build();
            });
        }
        if (checkOptions("time", "witp.time")) {
            builder.setItem(dynamic.next(), config.getFromItemData(locale, "options.time", time.toLowerCase()), (t, e) -> {
                List<String> pos = Arrays.asList("Day", "Noon", "Dawn", "Night", "Midnight");
                int i = 11;
                for (String time : pos) {
                    timeofday.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + time).build(), (t2, e2) -> {
                        if (e2.getItemMeta() != null) {
                            String name = ChatColor.stripColor(e2.getItemMeta().getDisplayName());
                            this.time = name;
                            sendTranslated("selected-time", time.toLowerCase());
                            player.setPlayerTime(getTime(name), false);
                        }
                    });
                    i++;
                }
                timeofday.setItem(26, close, (t2, e2) -> menu());
                timeofday.build();
            });
        }
        ItemStack item;
        if (checkOptions("difficulty", "witp.difficulty")) {
            builder.setItem(dynamic.next(),
                    config.getFromItemData(locale, "options.difficulty", "&a" + Util.parseDifficulty(difficulty) + " &7(" + calculateDifficultyScore() + "/1.0)"),
                    (t2, e2) -> difficultyMenu());
        }
        if (checkOptions("particles", "witp.particles")) {
            String particlesString = Boolean.toString(useParticles);
            item = config.getFromItemData(locale, "options.particles", normalizeBoolean(Util.colorBoolean(particlesString)));
            item.setType(useParticles ? Material.GREEN_WOOL : Material.RED_WOOL);
            builder.setItem(dynamic.next(), item, (t2, e2) -> {
                useParticles = !useParticles;
                sendTranslated("selected-particles", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
                menu();
            });
        }
        if (checkOptions("scoreboard", "witp.scoreboard") && Option.SCOREBOARD) {
            String scoreboardString = Boolean.toString(showScoreboard);
            item = config.getFromItemData(locale, "options.scoreboard", normalizeBoolean(Util.colorBoolean(scoreboardString)));
            item.setType(showScoreboard ? Material.GREEN_WOOL : Material.RED_WOOL);
            builder.setItem(dynamic.next(), item, (t2, e2) -> {
                showScoreboard = !showScoreboard;
                if (showScoreboard) {
                    board = new FastBoard(player);
                    updateScoreboard();
                } else {
                    board.delete();
                }
                sendTranslated("selected-scoreboard", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(scoreboardString))));
                menu();
            });
        }
        if (checkOptions("death-msg", "witp.fall")) {
            String deathString = Boolean.toString(showDeathMsg);
            item = config.getFromItemData(locale, "options.death-msg", normalizeBoolean(Util.colorBoolean(deathString)));
            item.setType(showDeathMsg ? Material.GREEN_WOOL : Material.RED_WOOL);
            builder.setItem(dynamic.next(), item, (t2, e2) -> {
                showDeathMsg = !showDeathMsg;
                sendTranslated("selected-fall-message", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
                menu();
            });
        }
        if (checkOptions("special", "witp.special")) {
            String specialString = Boolean.toString(useSpecial);
            item = config.getFromItemData(locale, "options.special", normalizeBoolean(Util.colorBoolean(specialString)));
            item.setType(useSpecial ? Material.GREEN_WOOL : Material.RED_WOOL);
            builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("special"));
        }
        if (checkOptions("structure", "witp.structures")) {
            String structuresString = Boolean.toString(useStructure);
            item = config.getFromItemData(locale, "options.structure", normalizeBoolean(Util.colorBoolean(structuresString)));
            item.setType(useStructure ? Material.GREEN_WOOL : Material.RED_WOOL);
            builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("structure"));
        }

        builder.setItem(18, WITP.getConfiguration().getFromItemData(locale, "options.gamemode"), (t2, e2) -> {
            if (checkOptions("gamemode", "witp.gamemode")) {
                gamemode();
            }
        });
        Integer score = highScores.get(uuid);
        builder.setItem(19, WITP.getConfiguration().getFromItemData(locale, "options.leaderboard",
                getTranslated("your-rank", Integer.toString(getRank(uuid)),
                Integer.toString(score == null ? 0 : score))), (t2, e2) -> {
            if (checkOptions("leaderboard", "witp.leaderboard")) {
                leaderboard(this, player, 1);
                player.closeInventory();
            }
        });
        builder.setItem(22, WITP.getConfiguration().getFromItemData(locale, "options.language", locale), (t2, e2) -> {
            if (checkOptions("language", "witp.language")) {
                List<String> langs = Option.LANGUAGES;
                InventoryBuilder.DynamicInventory dynamic1 = new InventoryBuilder.DynamicInventory(langs.size(), 1);
                for (String langName : langs) {
                    language.setItem(dynamic1.next(), new ItemBuilder(Material.PAPER, "&c" + langName).build(), (t3, e3) -> {
                        lang = langName;
                        locale = langName;
                        sendTranslated("selected-language", langName);
                    });
                }
                language.setItem(26, close, (t3, e3) -> menu());
                language.build();
            }
        });
        builder.setItem(26, WITP.getConfiguration().getFromItemData(locale, "general.quit"), (t2, e2) -> {
            player.closeInventory();
            try {
                sendTranslated("left");
                ParkourPlayer.unregister(this, true, true, true);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to quit player " + player.getName());
            }
        });
        builder.setItem(25, close, (t2, e2) -> player.closeInventory());
        builder.build();
    }

    private void difficultyMenu() {
        Configuration config = WITP.getConfiguration();
        InventoryBuilder difficulty = new InventoryBuilder(this, 3, "Difficulty").open();
        ItemStack close = config.getFromItemData(locale, "general.close");

        InventoryBuilder.DynamicInventory dynamic1 = new InventoryBuilder.DynamicInventory(5, 1);
        String difficultyString = Boolean.toString(useDifficulty);
        ItemStack diffSwitchItem = config.getFromItemData(locale, "options.difficulty-switch", normalizeBoolean(Util.colorBoolean(difficultyString)));
        diffSwitchItem.setType(useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL);
        int diffSlot = dynamic1.next();
        difficulty.setItem(diffSlot, diffSwitchItem, (t3, e3) -> {
            if (checkOptions("difficulty-switch", "witp.difficulty-switch")) {
                askReset("difficulty");
            }
        });
        difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.LIME_WOOL, "&a&l" + Util.capitalizeFirst(Util.parseDifficulty(0.3))).build(), (t3, e3) -> askReset("e-difficulty"));
        difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.GREEN_WOOL, "&2&l" + Util.capitalizeFirst(Util.parseDifficulty(0.5))).build(), (t3, e3) -> askReset("m-difficulty"));
        difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.ORANGE_WOOL, "&6&l" + Util.capitalizeFirst(Util.parseDifficulty(0.7))).build(), (t3, e3) -> askReset("h-difficulty"));
        difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.RED_WOOL, "&c&l" + Util.capitalizeFirst(Util.parseDifficulty(0.8))).build(), (t3, e3) -> askReset("vh-difficulty"));

        difficulty.setItem(26, close, (t3, e3) -> menu());
        difficulty.build();
    }

    private void askReset(String item) {
        if (generator.score < 25) {
            confirmReset(item);
            return;
        }
        sendTranslated("confirm");
        ComponentBuilder builder = new ComponentBuilder()
                .append(Util.color("&a&l" + getTranslated("true").toUpperCase()))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/witp askreset " + item + " true"))
                .append(Util.color(" &8| " + getTranslated("confirm-click")));
        player.spigot().sendMessage(builder.create());
        player.closeInventory();
    }

    public void confirmReset(String item) {
        switch (item) {
            case "structure":
                useStructure = !useStructure;
                sendTranslated("selected-structures", normalizeBoolean(Util.colorBoolean(Boolean.toString(useStructure))));
                menu();
                break;
            case "special":
                useSpecial = !useSpecial;
                sendTranslated("selected-special-blocks", normalizeBoolean(Util.colorBoolean(Boolean.toString(useSpecial))));
                menu();
                break;
            case "e-difficulty":
                difficulty = 0.3;
                sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(difficulty));
                break;
            case "m-difficulty":
                difficulty = 0.5;
                sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(difficulty));
                break;
            case "h-difficulty":
                difficulty = 0.7;
                sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(difficulty));
                break;
            case "vh-difficulty":
                difficulty = 0.8;
                sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(difficulty));
                break;
            case "difficulty":
                useDifficulty = !useDifficulty;
                sendTranslated("selected-difficulty", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(Boolean.toString(useDifficulty)))));
                difficultyMenu();
                break;
        }
    }

    private @Nullable List<Material> getPossibleMaterials(String style) {
        List<Material> possibleStyles = new ArrayList<>();
        String possible = WITP.getConfiguration().getFile("config").getString("styles.list." + style);
        if (possible == null) {
            Verbose.warn("Style selected (" + style + ") doesn't exist in config.yml, defaulting to ");
            return null;
        }
        String[] materials = possible.replaceAll("[\\[\\]]", "").split(", ");
        for (String material : materials) {
            Material mat = Material.getMaterial(material.toUpperCase());
            if (mat == null) {
                continue;
            }
            possibleStyles.add(mat);
        }

        return possibleStyles;
    }

    private boolean checkOptions(String option, @Nullable String perm) {
        boolean enabled = WITP.getConfiguration().getFile("items").getBoolean("items.options." + option + ".enabled");
        if (!enabled) {
            sendTranslated("cant-do");
            return false;
        } else {
            return checkPermission(perm);
        }
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
                if (Option.SQL) {
                    Verbose.verbose("Writing player's data to SQL server");

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
                Verbose.error("Error while trying to save the player's data..");
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
        if (Option.GAMELOGS && Option.SQL && generator.score > 0) {
            InsertStatement statement = new InsertStatement(WITP.getDatabase(), Option.SQL_PREFIX + "game-history")
                    .setValue("code", Util.randomOID()).setValue("uuid", uuid.toString())
                    .setValue("name", player.getName()).setValue("score", generator.score)
                    .setValue("hstime", generator.time).setValue("scoreDiff", calculateDifficultyScore());
            try {
                statement.query();
            } catch (InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while saving game");
            }
        }
    }

    /**
     * Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
     * the player to achieve this score using their settings.
     *
     * @return a number from 0 -> 1 (both inclusive)
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
    public static @Nullable ParkourPlayer register(@NotNull Player player) throws IOException, SQLException {
        if (!Option.JOINING) {
            player.sendMessage(Util.color("&c&l(!) &7Parkour is currently disabled. Try again later."));
            return null;
        }
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
            if (!Option.SQL) {
                File data = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid + ".json");
                if (data.exists()) {
                    Verbose.verbose("Reading player data..");
                    FileReader reader = new FileReader(data);
                    ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
                    if (from.useParticles == null) { // outdated file format
                        from.useParticles = true;
                    }
                    if (from.showDeathMsg == null) {
                        from.showDeathMsg = true;
                    }
                    if (from.useSpecial == null) {
                        from.useSpecial = true;
                    }
                    if (from.useStructure == null) {
                        from.useStructure = true;
                    }
                    if (from.showScoreboard == null) {
                        from.showScoreboard = true;
                    }
                    if (from.highScoreTime == null) {
                        from.highScoreTime = "0.0s";
                    }
                    if (from.difficulty == 0) {
                        from.difficulty = 0.5;
                    }
                    if (from.blockLead < 1) {
                        from.blockLead = 4;
                    }
                    if (from.lang == null) {
                        from.lang = Option.DEFAULT_LANG;
                    }
                    if (from.highScoreDifficulty == null) {
                        from.highScoreDifficulty = "?";
                    }
                    pp.setDefaults(from.highScore, from.time, from.style, from.highScoreTime, from.lang, from.blockLead,
                            from.useParticles, from.useDifficulty, from.useStructure, from.useSpecial, from.showDeathMsg, from.showScoreboard, from.highScoreDifficulty);
                    pp.saveStats();
                    players.put(pp.player, pp);
                    reader.close();
                } else {
                    Verbose.verbose("Setting new player data..");
                    pp.setDefaults(0, "Day", WITP.getConfiguration().getString("config", "styles.default"),
                            "0.0s", Option.DEFAULT_LANG,
                            4, true, true, true, true, true, true, "?");
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
                    pp.setDefaults(0, "Day", WITP.getConfiguration().getString("config", "styles.default"),
                            "0.0s", Option.DEFAULT_LANG, 4,
                            true, true, true, true, true, true, "?");
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
                            Option.DEFAULT_LANG,
                            Integer.parseInt((String) objects.get(2)), translateSqlBoolean((String) objects.get(3)),
                            translateSqlBoolean((String) objects.get(4)), translateSqlBoolean((String) objects.get(5)),
                            translateSqlBoolean((String) objects.get(6)), translateSqlBoolean((String) objects.get(7)),
                            translateSqlBoolean((String) objects.get(8)), highScoreDifficulty);
                } else {
                    pp.setDefaults(highscore, "Day", WITP.getConfiguration().getString("config", "styles.default"),
                            highScoreTime, Option.DEFAULT_LANG, 4,
                            true, true, true, true, true, true, "?");
                    pp.saveStats();
                }
                players.put(pp.player, pp);
            }
            return pp;
        }
        JOIN_COUNT++;
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
            Verbose.error("Time is null, defaulting to daytime");
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

    /**
     * Gets the player's {@link ParkourGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public ParkourGenerator getGenerator() {
        return generator;
    }

    /**
     * Makes a boolean readable for normal players
     *
     * @param   value
     *          The value
     *
     * @return true -> yes, false -> no
     */
    public String normalizeBoolean(String value) {
        return value.replaceAll("true", getTranslated("true")).replaceAll("false", getTranslated("false"));
    }
}

