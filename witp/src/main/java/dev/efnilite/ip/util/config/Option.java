package dev.efnilite.ip.util.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.particle.ParticleData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Class for variables required in generating without accessing the file a lot (constants)
 */
public class Option {

    private static FileConfiguration generation;
    private static FileConfiguration config;
    private static FileConfiguration lang;
    private static FileConfiguration items;
    private static FileConfiguration scoreboard;

    public static boolean AUTO_UPDATER;

    // Config stuff
    public static boolean ALL_POINTS;
    public static boolean REWARDS_USE_TOTAL_SCORE;

    public static boolean INVENTORY_HANDLING;
    public static boolean PERMISSIONS;
    public static boolean FOCUS_MODE;
    public static List<String> FOCUS_MODE_WHITELIST;
    public static boolean GO_BACK;
    public static boolean BUNGEECORD;

    public static List<Integer> POSSIBLE_LEADS;
    // Advanced settings
    public static String HEADING;

    public static List<String> LANGUAGES;
    public static String DEFAULT_LOCALE;
    public static boolean JOIN_LEAVE_MESSAGES;

    public static String DEFAULT_STYLE;

    public static boolean ENABLE_JOINING;
    public static boolean PERMISSIONS_STYLES;
    public static boolean SAVE_STATS;
    public static boolean SETTINGS_ENABLED;
    public static boolean HEALTH_HANDLING;
    public static boolean INVENTORY_SAVING;
    public static String ALT_INVENTORY_SAVING_COMMAND;

    public static int OPTIONS_TIME_FORMAT;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String> OPTIONS_DEFAULTS;

    // Worlds
    public static boolean DELETE_ON_RELOAD;
    public static String WORLD_NAME;

    public static void init(boolean firstLoad) {
        scoreboard = IP.getConfiguration().getFile("scoreboard");
        generation = IP.getConfiguration().getFile("generation");
        config = IP.getConfiguration().getFile("config");
        lang = IP.getConfiguration().getFile("lang");
        items = IP.getConfiguration().getFile("items");

        initScoreboard();
        initSql();
        initEnums();
        initGeneration();
        initAdvancedGeneration();

        // General settings
        AUTO_UPDATER = config.getBoolean("auto-updater");
        ENABLE_JOINING = config.getBoolean("joining");
        JOIN_LEAVE_MESSAGES = config.getBoolean("join-leave-messages");

        // Worlds
        DELETE_ON_RELOAD = config.getBoolean("world.delete-on-reload");
        WORLD_NAME = config.getString("world.name");

        if (!WORLD_NAME.matches("[a-zA-Z0-9/._-]+")) {
            IP.logging().stack("Invalid world name!", "world names need to match regex \"[a-zA-Z0-9/._-]+\"");
        }

        // Options

        SAVE_STATS = config.getBoolean("options.save-stats");
        SETTINGS_ENABLED = config.getBoolean("options.enabled");
        OPTIONS_TIME_FORMAT = config.getInt("options.time.format");
        HEALTH_HANDLING = config.getBoolean("options.health-handling");
        INVENTORY_SAVING = config.getBoolean("options.inventory-saving");
        ALT_INVENTORY_SAVING_COMMAND = config.getString("options.alt-inventory-saving-command");

        List<ParkourOption> options = Arrays.asList(ParkourOption.LEADS, ParkourOption.TIME,
                ParkourOption.SCHEMATIC_DIFFICULTY, ParkourOption.SCORE_DIFFICULTY,
                ParkourOption.PARTICLES_AND_SOUND, ParkourOption.SHOW_SCOREBOARD,
                ParkourOption.SHOW_FALL_MESSAGE, ParkourOption.SPECIAL_BLOCKS,
                ParkourOption.USE_SCHEMATICS);

        // =====================================

        OPTIONS_DEFAULTS = new HashMap<>();
        OPTIONS_ENABLED = new HashMap<>();

        for (String node : Util.getNode(items, "items.options", false)) {
            for (ParkourOption option : options) {
                if (option.getName().equalsIgnoreCase(node)) {

                    // register default value
                    String value = items.getString("items.options." + node + ".default");
                    if (value == null) {
                        IP.logging().stack("Default option '" + node + "' is null!", "check the items file and the default options");
                        continue;
                    }
                    OPTIONS_DEFAULTS.put(option, value);

                    // register enabled value
                    boolean enabled = items.getBoolean("items.options." + node + ".enabled");

                    OPTIONS_ENABLED.put(option, enabled);
                }
            }
        }

        // =====================================

        PERMISSIONS_STYLES = config.getBoolean("permissions.per-style");

        LANGUAGES = new ArrayList<>(lang.getConfigurationSection("messages").getKeys(false));
        LANGUAGES.remove("default");

        DEFAULT_LOCALE = lang.getString("messages.default");

        DEFAULT_STYLE = config.getString("styles.default");

        // Config stuff

        POSSIBLE_LEADS = config.getIntegerList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1 || lead > 64) {
                IP.logging().error("Invalid lead in config: found " + lead + ", should be above 1 and below 64 to prevent lag.");
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        INVENTORY_HANDLING = config.getBoolean("options.inventory-handling");
        PERMISSIONS = config.getBoolean("permissions.enabled");
        FOCUS_MODE = config.getBoolean("focus-mode.enabled");
        FOCUS_MODE_WHITELIST = config.getStringList("focus-mode.whitelist");

        // Bungeecord
        GO_BACK = config.getBoolean("bungeecord.go-back-enabled");
        BUNGEECORD = config.getBoolean("bungeecord.enabled");

        // Generation
        HEADING = generation.getString("advanced.island.parkour.heading");

        // Scoring
        ALL_POINTS = config.getBoolean("scoring.all-points");
        REWARDS_USE_TOTAL_SCORE = config.getBoolean("scoring.rewards-use-total-score");

        if (firstLoad) {
            BORDER_SIZE = generation.getDouble("advanced.border-size");
            SQL = config.getBoolean("sql.enabled");
        }
    }

    public static ParticleShape PARTICLE_SHAPE;
    public static Sound SOUND_TYPE;
    public static int SOUND_PITCH;
    public static Particle PARTICLE_TYPE;
    public static ParticleData<?> PARTICLE_DATA;

    // Very not efficient but this is basically the only way to ensure the enums have a value
    private static void initEnums() {
        String value;
        value = config.getString("particles.sound-type").toUpperCase();

        try {
            SOUND_TYPE = Sound.valueOf(value);
        } catch (IllegalArgumentException ex) {
            try {
                SOUND_TYPE = Sound.valueOf("BLOCK_NOTE_PLING");
            } catch (IllegalArgumentException ex2) {
                IP.logging().error("Invalid sound: " + value);
                SOUND_TYPE = Sound.values()[0];
            }
        }

        value = config.getString("particles.particle-type");
        try {
            PARTICLE_TYPE = Particle.valueOf(value);
        } catch (IllegalArgumentException ex) {
            try {
                PARTICLE_TYPE = Particle.valueOf("SPELL_INSTANT");
            } catch (IllegalArgumentException ex2) {
                IP.logging().error("Invalid particle: " + value);
                PARTICLE_TYPE = Particle.values()[0];
            }
        }

        SOUND_PITCH = config.getInt("particles.sound-pitch");
        PARTICLE_SHAPE = ParticleShape.valueOf(config.getString("particles.particle-shape").toUpperCase());
        PARTICLE_DATA = new ParticleData<>(PARTICLE_TYPE, null, 10, 0, 0, 0, 0);
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

        if (MIN_Y >= MAX_Y) {
            IP.logging().stack("Provided minimum y is the same or larger than maximum y!", "check your generation.yml file");

            // prevent plugin breakage
            MIN_Y = 100;
            MAX_Y = 200;
        }
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
        MULTIPLIER = generation.getDouble("advanced.maxed-multiplier");

        MAXED_ONE_BLOCK = generation.getInt("advanced.maxed-values.1-block");
        MAXED_TWO_BLOCK = generation.getInt("advanced.maxed-values.2-block");
        MAXED_THREE_BLOCK = generation.getInt("advanced.maxed-values.3-block");
        MAXED_FOUR_BLOCK = generation.getInt("advanced.maxed-values.4-block");
    }

    // --------------------------------------------------------------
    // Scoreboard

    public static boolean SCOREBOARD_ENABLED;
    public static String SCOREBOARD_TITLE;
    public static List<String> SCOREBOARD_LINES;

    private static void initScoreboard() {
        SCOREBOARD_ENABLED = scoreboard.getBoolean("scoreboard.enabled");
        SCOREBOARD_TITLE = Util.color(scoreboard.getString("scoreboard.title"));
        SCOREBOARD_LINES = Util.colorList(scoreboard.getStringList("scoreboard.lines"));
    }

    public enum ParticleShape {
        DOT,
        CIRCLE,
        BOX
    }
}