package dev.efnilite.witp.util.config;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.generator.subarea.Direction;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.Version;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class for variables required in generating without accessing the file a lot (constants)
 */
public class Option {

    private static FileConfiguration generation;
    private static FileConfiguration config;
    private static FileConfiguration lang;
    private static FileConfiguration items;

    // Config stuff
    public static boolean INVENTORY_HANDLING;
    public static boolean PERMISSIONS;
    public static boolean FOCUS_MODE;
    public static List<String> FOCUS_MODE_WHITELIST;
    public static boolean GO_BACK;
    public static boolean BUNGEECORD;

    public static List<Integer> POSSIBLE_LEADS;
    public static boolean VERBOSE;
    public static boolean GAMELOGS;

    public static boolean UPDATER;

    // Advanced settings
    public static Direction HEADING;

    public static List<String> LANGUAGES;
    public static String DEFAULT_LANG;
    public static boolean JOIN_LEAVE;

    public static String DEFAULT_STYLE;
    public static List<String> STYLES;

    public static boolean JOINING;
    public static boolean PERMISSIONS_STYLES;
    public static boolean SAVE_STATS;
    public static boolean LEAVE_REWARDS;
    public static boolean OPTIONS_ENABLED;

    public static HashMap<String, String> OPTIONS_DEFAULTS;
    public static boolean HOTBAR_QUIT_ITEM;

    public static void init(boolean init) {
        generation = WITP.getConfiguration().getFile("generation");
        config = WITP.getConfiguration().getFile("config");
        lang = WITP.getConfiguration().getFile("lang");
        items = WITP.getConfiguration().getFile("items");

        initSql();
        initEnums();
        initRewards();
        initScoreboard();
        initGeneration();
        initAdvancedGeneration();

        List<String> options = Arrays.asList("lead", "time", "difficulty", "schematic-difficulty", "adaptive-difficulty", "particles", "scoreboard", "death-msg", "special", "structure");
        OPTIONS_DEFAULTS = new HashMap<>();
        for (String node : Util.getNode(items, "items.options")) {
            for (String option : options) {
                if (option.equalsIgnoreCase(node)) {
                    OPTIONS_DEFAULTS.put(node, items.getString("items.options." + node + ".default"));
                }
            }
        }

        HOTBAR_QUIT_ITEM = config.getBoolean("options.hotbar-quit-item");

        JOIN_LEAVE = lang.getBoolean("messages.join-leave-enabled");
        HEADING = Util.getDirection(generation.getString("advanced.island.parkour.heading"));
        PERMISSIONS_STYLES = config.getBoolean("permissions.per-style");
        GAMELOGS = config.getBoolean("sql.game-logs");
        LANGUAGES = new ArrayList<>(lang.getConfigurationSection("messages").getKeys(false));
        LANGUAGES.remove("default");
        DEFAULT_LANG = lang.getString("messages.default");

        DEFAULT_STYLE = config.getString("styles.default");
        STYLES = Util.getNode(config, "styles.list");
        if (STYLES == null) {
            Verbose.error("Error while trying to fetch possible styles from config.yml");
        }

        SAVE_STATS = config.getBoolean("options.save-stats");
        OPTIONS_ENABLED = config.getBoolean("options.enabled");

        UPDATER = config.getBoolean("update-checker");
        JOINING = config.getBoolean("joining");
        VERBOSE = config.getBoolean("verbose");

        // Config stuff

        POSSIBLE_LEADS = config.getIntegerList("lead.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1) {
                Verbose.error("Invalid lead in config: found " + lead + ", should be >1");
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        INVENTORY_HANDLING = config.getBoolean("options.inventory-handling");
        PERMISSIONS = config.getBoolean("permissions.enabled");
        FOCUS_MODE = config.getBoolean("focus-mode.enabled");
        FOCUS_MODE_WHITELIST = config.getStringList("focus-mode.whitelist");

        GO_BACK = config.getBoolean("bungeecord.go-back-enabled");

        if (init) {
            BORDER_SIZE = generation.getDouble("advanced.border-size");
            SQL = config.getBoolean("sql.enabled");
        }

        SchematicCache.read();
    }

    public static Option.ParticleShape PARTICLE_SHAPE;
    public static Sound SOUND_TYPE;
    public static int SOUND_PITCH;
    public static Particle PARTICLE_TYPE;

    // Very not efficient but this is basically the only way to ensure the enums have a value
    private static void initEnums() {
        String enumValue;
        enumValue = config.getString("particles.sound-type").toUpperCase();

        if (Version.isHigherOrEqual(Version.V1_9)) { // 1.8 has no Particle class & severely limited Sound support
            try {
                SOUND_TYPE = Sound.valueOf(enumValue);
            } catch (IllegalArgumentException ex) {
                try {
                    SOUND_TYPE = Sound.valueOf("BLOCK_NOTE_PLING");
                } catch (IllegalArgumentException ex2) {
                    Verbose.error("Invalid sound: " + enumValue);
                    SOUND_TYPE = Sound.values()[0];
                }
            }
            SOUND_PITCH = config.getInt("particles.sound-pitch");

            try {
                PARTICLE_TYPE = Particle.valueOf(enumValue);
            } catch (IllegalArgumentException ex) {
                try {
                    PARTICLE_TYPE = Particle.valueOf("SPELL_INSTANT");
                } catch (IllegalArgumentException ex2) {
                    Verbose.error("Invalid particle: " + enumValue);
                    PARTICLE_TYPE = Particle.values()[0];
                }
            }

            PARTICLE_SHAPE = ParticleShape.valueOf(config.getString("particles.particle-shape").toUpperCase());
        }
    }


    // --------------------------------------------------------------
    // MySQL
    public static boolean SQL;
    public static int SQL_PORT;
    public static String SQL_URL;
    public static String SQL_DB;
    public static String SQL_USERNAME;
    public static String SQL_PASSWORD;
    public static String SQL_PREFIX;

    private static void initSql() {
        SQL_PORT = config.getInt("sql.port");
        SQL_DB = config.getString("sql.database");
        SQL_URL = config.getString("sql.url");
        SQL_USERNAME = config.getString("sql.username");
        SQL_PASSWORD = config.getString("sql.password");
        SQL_PREFIX = config.getString("sql.prefix");
    }

    // --------------------------------------------------------------
    // Generation

    public static int NORMAL;
    public static int SPECIAL;
    public static int SCHEMATICS;

    public static int SPECIAL_ICE;
    public static int SPECIAL_SLAB;
    public static int SPECIAL_PANE;
    public static int SPECIAL_FENCE;

    public static int NORMAL_ONE_BLOCK;
    public static int NORMAL_TWO_BLOCK;
    public static int NORMAL_THREE_BLOCK;
    public static int NORMAL_FOUR_BLOCK;

    public static int NORMAL_UP;
    public static int NORMAL_LEVEL;
    public static int NORMAL_DOWN;
    public static int NORMAL_DOWN2;

    public static int MAX_Y;
    public static int MIN_Y;

    private static void initGeneration() {
        NORMAL = generation.getInt("generation.normal-jump.chance");
        SCHEMATICS = generation.getInt("generation.structures.chance");
        SPECIAL = generation.getInt("generation.normal-jump.special.chance");

        SPECIAL_ICE = generation.getInt("generation.normal-jump.special.ice");
        SPECIAL_SLAB = generation.getInt("generation.normal-jump.special.slab");
        SPECIAL_PANE = generation.getInt("generation.normal-jump.special.pane");
        SPECIAL_FENCE = generation.getInt("generation.normal-jump.special.fence");

        NORMAL_ONE_BLOCK = generation.getInt("generation.normal-jump.1-block");
        NORMAL_TWO_BLOCK = generation.getInt("generation.normal-jump.2-block");
        NORMAL_THREE_BLOCK = generation.getInt("generation.normal-jump.3-block");
        NORMAL_FOUR_BLOCK = generation.getInt("generation.normal-jump.4-block");

        NORMAL_UP = generation.getInt("generation.normal-jump.up");
        NORMAL_LEVEL = generation.getInt("generation.normal-jump.level");
        NORMAL_DOWN = generation.getInt("generation.normal-jump.down");
        NORMAL_DOWN2 = generation.getInt("generation.normal-jump.down2");

        MAX_Y = generation.getInt("generation.settings.max-y");
        MIN_Y = generation.getInt("generation.settings.min-y");
    }

    // --------------------------------------------------------------
    // Advanced settings in generation

    public static double BORDER_SIZE;
    public static int GENERATOR_CHECK;
    public static double HEIGHT_GAP;
    public static double MULTIPLIER;

    public static int MAXED_ONE_BLOCK;
    public static int MAXED_TWO_BLOCK;
    public static int MAXED_THREE_BLOCK;
    public static int MAXED_FOUR_BLOCK;

    private static void initAdvancedGeneration() {
        GENERATOR_CHECK = generation.getInt("advanced.generator-check");
        HEIGHT_GAP = generation.getDouble("advanced.height-gap");
        MULTIPLIER = generation.getInt("advanced.maxed-multiplier");

        MAXED_ONE_BLOCK = generation.getInt("advanced.maxed-values.1-block");
        MAXED_TWO_BLOCK = generation.getInt("advanced.maxed-values.2-block");
        MAXED_THREE_BLOCK = generation.getInt("advanced.maxed-values.3-block");
        MAXED_FOUR_BLOCK = generation.getInt("advanced.maxed-values.4-block");
    }

    // --------------------------------------------------------------
    // Rewards

    public static boolean ALL_POINTS;
    public static boolean REWARDS;
    public static HashMap<Integer, List<String>> REWARDS_SCORES;
    public static HashMap<Integer, List<String>> ON_LEAVE_REWARDS_SCORES;
    public static List<String> INTERVAL_REWARDS_SCORES;
    public static int REWARDS_INTERVAL;
    public static double REWARDS_MONEY;
    public static String REWARDS_COMMAND;
    public static String REWARDS_MESSAGE;

    private static void initRewards() {
        List<String> intervals = config.getStringList("rewards.scores");
        REWARDS_SCORES = new HashMap<>();
        for (String key : intervals) {
            String[] values = key.split(";;");
            if (values.length > 1) {
                List<String> commands = new ArrayList<>(Arrays.asList(values));
                commands.remove(0);
                REWARDS_SCORES.put(Integer.parseInt(values[0]), commands);
            } else {
                REWARDS_SCORES.put(Integer.parseInt(values[0]), null);
            }
        }

        List<String> intervals1 = config.getStringList("rewards.on-leave-scores");
        ON_LEAVE_REWARDS_SCORES = new HashMap<>();
        for (String key : intervals1) {
            String[] values = key.split(";;");
            if (values.length > 1) {
                List<String> commands = new ArrayList<>(Arrays.asList(values));
                commands.remove(0);
                ON_LEAVE_REWARDS_SCORES.put(Integer.parseInt(values[0]), commands);
            } else {
                ON_LEAVE_REWARDS_SCORES.put(Integer.parseInt(values[0]), null);
            }
        }

        List<String> intervals2 = config.getStringList("rewards.command");
        INTERVAL_REWARDS_SCORES = new ArrayList<>();
        INTERVAL_REWARDS_SCORES.addAll(intervals2);

        BUNGEECORD = config.getBoolean("bungeecord.enabled");
        REWARDS = config.getBoolean("rewards.enabled");
        REWARDS_MONEY = config.getInt("rewards.vault-reward");
        REWARDS_INTERVAL = config.getInt("rewards.interval");
        REWARDS_COMMAND = config.getString("rewards.command").replace("/", "");
        if (REWARDS_COMMAND.equalsIgnoreCase("null")) {
            REWARDS_COMMAND = null;
        }
        REWARDS_MESSAGE = config.getString("rewards.message");
        if (REWARDS_MESSAGE.equalsIgnoreCase("null") || REWARDS_MESSAGE.equals("''") || REWARDS_MESSAGE.equals("")) {
            REWARDS_MESSAGE = null;
        }

        LEAVE_REWARDS = config.getBoolean("rewards.leave-rewards");
        ALL_POINTS = config.getBoolean("scoring.all-points");
    }

    // --------------------------------------------------------------
    // Scoreboard

    public static boolean SCOREBOARD;
    public static String SCOREBOARD_TITLE;
    public static List<String> SCOREBOARD_LINES;

    private static void initScoreboard() {
        SCOREBOARD = lang.getBoolean("scoreboard.enabled");
        SCOREBOARD_TITLE = Util.color(lang.getString("scoreboard.title"));
        SCOREBOARD_LINES = Util.color(lang.getStringList("scoreboard.lines"));
    }

    public enum ParticleShape {
        DOT,
        CIRCLE,
        BOX
    }
}
