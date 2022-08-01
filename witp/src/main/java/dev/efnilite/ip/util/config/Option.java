package dev.efnilite.ip.util.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.config.ConfigOption;
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
    public static ConfigOption<Boolean> ALL_POINTS;
    public static ConfigOption<Boolean> REWARDS_USE_TOTAL_SCORE;

    public static ConfigOption<Boolean> INVENTORY_HANDLING;
    public static ConfigOption<Boolean> PERMISSIONS;
    public static ConfigOption<Boolean> FOCUS_MODE;
    public static ConfigOption<List<String>> FOCUS_MODE_WHITELIST;
    public static ConfigOption<Boolean> GO_BACK;
    public static ConfigOption<Boolean> BUNGEECORD;

    public static List<Integer> POSSIBLE_LEADS;
    // Advanced settings
    public static String HEADING;

    public static ConfigOption<List<String>> LANGUAGES;
    public static String DEFAULT_LOCALE;
    public static ConfigOption<Boolean> JOIN_LEAVE_MESSAGES;

    public static String DEFAULT_STYLE;

    public static ConfigOption<Boolean> ENABLE_JOINING;
    public static ConfigOption<Boolean> PERMISSIONS_STYLES;
    public static ConfigOption<Boolean> SAVE_STATS;
    public static ConfigOption<Boolean> SETTINGS_ENABLED;
    public static ConfigOption<Boolean> HEALTH_HANDLING;
    public static ConfigOption<Boolean> INVENTORY_SAVING;
    public static ConfigOption<String> ALT_INVENTORY_SAVING_COMMAND;

    public static ConfigOption<Integer> OPTIONS_TIME_FORMAT;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String> OPTIONS_DEFAULTS;
    public static ConfigOption<Boolean> HOTBAR_QUIT_ITEM;

    // Worlds
    public static ConfigOption<Boolean> DELETE_ON_RELOAD;
    public static ConfigOption<String> WORLD_NAME;

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
        ENABLE_JOINING = new ConfigOption<>(config, "joining");
        JOIN_LEAVE_MESSAGES = new ConfigOption<>(config, "join-leave-messages");

        // Worlds
        DELETE_ON_RELOAD = new ConfigOption<>(config, "world.delete-on-reload");
        WORLD_NAME = new ConfigOption<>(config, "world.name", "[a-zA-Z0-9/._-]+");

        // Options

        SAVE_STATS = new ConfigOption<>(config, "options.save-stats");
        SETTINGS_ENABLED = new ConfigOption<>(config, "options.enabled");
        OPTIONS_TIME_FORMAT = new ConfigOption<>(config, "options.time.format");
        HEALTH_HANDLING = new ConfigOption<>(config, "options.health-handling");
        INVENTORY_SAVING = new ConfigOption<>(config, "options.inventory-saving");
        ALT_INVENTORY_SAVING_COMMAND = new ConfigOption<>(config, "options.alt-inventory-saving-command");

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

        HOTBAR_QUIT_ITEM = new ConfigOption<>(config, "options.hotbar-quit-item");

        PERMISSIONS_STYLES = new ConfigOption<>(config, "permissions.per-style");

        LANGUAGES = new ConfigOption<>(new ArrayList<>(lang.getConfigurationSection("messages").getKeys(false)));
        List<String> languages = new ArrayList<>(LANGUAGES.get());
        languages.remove("default");
        LANGUAGES.thenSet(languages);

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

        INVENTORY_HANDLING = new ConfigOption<>(config, "options.inventory-handling");
        PERMISSIONS = new ConfigOption<>(config, "permissions.enabled");
        FOCUS_MODE = new ConfigOption<>(config, "focus-mode.enabled");
        FOCUS_MODE_WHITELIST = new ConfigOption<>(config, "focus-mode.whitelist");

        // Bungeecord
        GO_BACK = new ConfigOption<>(config, "bungeecord.go-back-enabled");
        BUNGEECORD = new ConfigOption<>(config, "bungeecord.enabled");

        // Generation
        HEADING = generation.getString("advanced.island.parkour.heading");

        // Scoring
        ALL_POINTS = new ConfigOption<>(config, "scoring.all-points");
        REWARDS_USE_TOTAL_SCORE = new ConfigOption<>(config, "scoring.rewards-use-total-score");

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

    public static ConfigOption<Integer> NORMAL;
    public static ConfigOption<Integer> SPECIAL;
    public static ConfigOption<Integer> SCHEMATICS;

    public static ConfigOption<Integer> SPECIAL_ICE;
    public static ConfigOption<Integer> SPECIAL_SLAB;
    public static ConfigOption<Integer> SPECIAL_PANE;
    public static ConfigOption<Integer> SPECIAL_FENCE;

    public static ConfigOption<Integer> NORMAL_ONE_BLOCK;
    public static ConfigOption<Integer> NORMAL_TWO_BLOCK;
    public static ConfigOption<Integer> NORMAL_THREE_BLOCK;
    public static ConfigOption<Integer> NORMAL_FOUR_BLOCK;

    public static ConfigOption<Integer> NORMAL_UP;
    public static ConfigOption<Integer> NORMAL_LEVEL;
    public static ConfigOption<Integer> NORMAL_DOWN;
    public static ConfigOption<Integer> NORMAL_DOWN2;

    public static int MAX_Y;
    public static int MIN_Y;

    private static void initGeneration() {
        NORMAL = new ConfigOption<>(generation, "generation.normal-jump.chance");
        SCHEMATICS = new ConfigOption<>(generation, "generation.structures.chance");
        SPECIAL = new ConfigOption<>(generation, "generation.normal-jump.special.chance");

        SPECIAL_ICE = new ConfigOption<>(generation, "generation.normal-jump.special.ice");
        SPECIAL_SLAB = new ConfigOption<>(generation, "generation.normal-jump.special.slab");
        SPECIAL_PANE = new ConfigOption<>(generation, "generation.normal-jump.special.pane");
        SPECIAL_FENCE = new ConfigOption<>(generation, "generation.normal-jump.special.fence");

        NORMAL_ONE_BLOCK = new ConfigOption<>(generation, "generation.normal-jump.1-block");
        NORMAL_TWO_BLOCK = new ConfigOption<>(generation, "generation.normal-jump.2-block");
        NORMAL_THREE_BLOCK = new ConfigOption<>(generation, "generation.normal-jump.3-block");
        NORMAL_FOUR_BLOCK = new ConfigOption<>(generation, "generation.normal-jump.4-block");

        NORMAL_UP = new ConfigOption<>(generation, "generation.normal-jump.up");
        NORMAL_LEVEL = new ConfigOption<>(generation, "generation.normal-jump.level");
        NORMAL_DOWN = new ConfigOption<>(generation, "generation.normal-jump.down");
        NORMAL_DOWN2 = new ConfigOption<>(generation, "generation.normal-jump.down2");

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
    public static ConfigOption<Integer> GENERATOR_CHECK;
    public static ConfigOption<Double> HEIGHT_GAP;
    public static ConfigOption<Double> MULTIPLIER;

    public static ConfigOption<Integer> MAXED_ONE_BLOCK;
    public static ConfigOption<Integer> MAXED_TWO_BLOCK;
    public static ConfigOption<Integer> MAXED_THREE_BLOCK;
    public static ConfigOption<Integer> MAXED_FOUR_BLOCK;

    private static void initAdvancedGeneration() {
        GENERATOR_CHECK = new ConfigOption<>(generation, "advanced.generator-check");
        HEIGHT_GAP = new ConfigOption<>(generation, "advanced.height-gap");
        MULTIPLIER = new ConfigOption<>(generation, "advanced.maxed-multiplier");

        MAXED_ONE_BLOCK = new ConfigOption<>(generation, "advanced.maxed-values.1-block");
        MAXED_TWO_BLOCK = new ConfigOption<>(generation, "advanced.maxed-values.2-block");
        MAXED_THREE_BLOCK = new ConfigOption<>(generation, "advanced.maxed-values.3-block");
        MAXED_FOUR_BLOCK = new ConfigOption<>(generation, "advanced.maxed-values.4-block");
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