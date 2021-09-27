package dev.efnilite.witp.util.config;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
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

    public static int NORMAL;
    public static int SPECIAL;
    public static int STRUCTURES;

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

    // Config stuff
    public static boolean REWARDS;
    public static HashMap<Integer, List<String>> REWARDS_SCORES;
    public static HashMap<Integer, List<String>> ON_LEAVE_REWARDS_SCORES;
    public static List<String> INTERVAL_REWARDS_SCORES;
    public static int REWARDS_INTERVAL;
    public static double REWARDS_MONEY;
    public static String REWARDS_COMMAND;
    public static String REWARDS_MESSAGE;

    public static Option.ParticleShape PARTICLE_SHAPE;
    public static Sound SOUND_TYPE;
    public static int SOUND_PITCH;
    public static Particle PARTICLE_TYPE;

    public static boolean SCOREBOARD;
    public static boolean INVENTORY_HANDLING;
    public static String SCOREBOARD_TITLE;
    public static List<String> SCOREBOARD_LINES;
    public static boolean PERMISSIONS;
    public static boolean FOCUS_MODE;
    public static List<String> FOCUS_MODE_WHITELIST;
    public static boolean GO_BACK;
    public static boolean BUNGEECORD;
    public static boolean JOIN_LEAVE;

    public static List<Integer> POSSIBLE_LEADS;
    public static boolean VERBOSE;
    public static List<String> LANGUAGES;
    public static boolean GAMELOGS;

    public static boolean UPDATER;

    // MySQL
    public static boolean SQL;
    public static int SQL_PORT;
    public static String SQL_URL;
    public static String SQL_DB;
    public static String SQL_USERNAME;
    public static String SQL_PASSWORD;
    public static String SQL_PREFIX;

    // Advanced settings
    public static double BORDER_SIZE;
    public static int GENERATOR_CHECK;
    public static double HEIGHT_GAP;
    public static double MULTIPLIER;

    public static int MAXED_ONE_BLOCK;
    public static int MAXED_TWO_BLOCK;
    public static int MAXED_THREE_BLOCK;
    public static int MAXED_FOUR_BLOCK;

    public static String DEFAULT_LANG;
    public static String DEFAULT_STYLE;

    public static boolean JOINING;

    public static void init(boolean init) {
        SchematicCache.read();

        FileConfiguration gen = WITP.getConfiguration().getFile("generation");
        FileConfiguration config = WITP.getConfiguration().getFile("config");
        FileConfiguration lang = WITP.getConfiguration().getFile("lang");

        GAMELOGS = config.getBoolean("sql.game-logs");
        LANGUAGES = new ArrayList<>(lang.getConfigurationSection("messages").getKeys(false));
        LANGUAGES.remove("default");
        DEFAULT_LANG = lang.getString("messages.default");
        DEFAULT_STYLE = config.getString("styles.default");

        UPDATER = config.getBoolean("update-checker");

        JOINING = config.getBoolean("joining");

        VERBOSE = config.getBoolean("verbose");

        SQL_PORT = config.getInt("sql.port");
        SQL_DB = config.getString("sql.database");
        SQL_URL = config.getString("sql.url");
        SQL_USERNAME = config.getString("sql.username");
        SQL_PASSWORD = config.getString("sql.password");
        SQL_PREFIX = config.getString("sql.prefix");

        NORMAL = gen.getInt("generation.normal-jump.chance");
        STRUCTURES = gen.getInt("generation.structures.chance");
        SPECIAL = gen.getInt("generation.normal-jump.special.chance");

        SPECIAL_ICE = gen.getInt("generation.normal-jump.special.ice");
        SPECIAL_SLAB = gen.getInt("generation.normal-jump.special.slab");
        SPECIAL_PANE = gen.getInt("generation.normal-jump.special.pane");
        SPECIAL_FENCE = gen.getInt("generation.normal-jump.special.fence");

        NORMAL_ONE_BLOCK = gen.getInt("generation.normal-jump.1-block");
        NORMAL_TWO_BLOCK = gen.getInt("generation.normal-jump.2-block");
        NORMAL_THREE_BLOCK = gen.getInt("generation.normal-jump.3-block");
        NORMAL_FOUR_BLOCK = gen.getInt("generation.normal-jump.4-block");

        NORMAL_UP = gen.getInt("generation.normal-jump.up");
        NORMAL_LEVEL = gen.getInt("generation.normal-jump.level");
        NORMAL_DOWN = gen.getInt("generation.normal-jump.down");
        NORMAL_DOWN2 = gen.getInt("generation.normal-jump.down2");

        MAX_Y = gen.getInt("generation.settings.max-y");
        MIN_Y = gen.getInt("generation.settings.min-y");

        // Config stuff
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

        JOIN_LEAVE = lang.getBoolean("messages.join-leave-enabled");
        BUNGEECORD = config.getBoolean("bungeecord.enabled");
        REWARDS = config.getBoolean("rewards.enabled");
        REWARDS_MONEY = config.getInt("rewards.vault-reward");
        REWARDS_INTERVAL = config.getInt("rewards.interval");
        REWARDS_COMMAND = config.getString("rewards.command").replaceAll("/", "");
        if (REWARDS_COMMAND.equalsIgnoreCase("null")) {
            REWARDS_COMMAND = null;
        }
        REWARDS_MESSAGE = config.getString("rewards.message");
        if (REWARDS_MESSAGE.equalsIgnoreCase("null") || REWARDS_MESSAGE.equals("''") || REWARDS_MESSAGE.equals("")) {
            REWARDS_MESSAGE = null;
        }

        POSSIBLE_LEADS = config.getIntegerList("lead.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1) {
                Verbose.error("Invalid lead in config: found " + lead + ", should be >1");
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        SCOREBOARD = lang.getBoolean("scoreboard.enabled");
        SCOREBOARD_TITLE = Util.color(lang.getString("scoreboard.title"));
        SCOREBOARD_LINES = Util.color(lang.getStringList("scoreboard.lines"));
        INVENTORY_HANDLING = config.getBoolean("options.inventory-handling");
        PERMISSIONS = config.getBoolean("permissions.enabled");
        FOCUS_MODE = config.getBoolean("focus-mode.enabled");
        FOCUS_MODE_WHITELIST = config.getStringList("focus-mode.whitelist");

        GO_BACK = config.getBoolean("bungeecord.go-back-enabled");

        SOUND_TYPE = Sound.valueOf(config.getString("particles.sound-type").toUpperCase());
        SOUND_PITCH = config.getInt("particles.sound-pitch");
        PARTICLE_TYPE = Particle.valueOf(config.getString("particles.particle-type").toUpperCase());
        PARTICLE_SHAPE = Option.ParticleShape.valueOf(config.getString("particles.particle-shape").toUpperCase());

        // Advanced settings
        if (init) {
            BORDER_SIZE = gen.getDouble("advanced.border-size");
            SQL = config.getBoolean("sql.enabled");
        }
        GENERATOR_CHECK = gen.getInt("advanced.generator-check");
        HEIGHT_GAP = gen.getDouble("advanced.height-gap");
        MULTIPLIER = gen.getInt("advanced.maxed-multiplier");

        MAXED_ONE_BLOCK = gen.getInt("advanced.maxed-values.1-block");
        MAXED_TWO_BLOCK = gen.getInt("advanced.maxed-values.2-block");
        MAXED_THREE_BLOCK = gen.getInt("advanced.maxed-values.3-block");
        MAXED_FOUR_BLOCK = gen.getInt("advanced.maxed-values.4-block");
    }

    public enum ParticleShape {
        DOT,
        CIRCLE,
        BOX
    }
}
