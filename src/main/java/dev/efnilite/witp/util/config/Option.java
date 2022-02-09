package dev.efnilite.witp.util.config;

import dev.efnilite.fycore.chat.ChatColour;
import dev.efnilite.fycore.config.ConfigOption;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Version;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.generator.subarea.Direction;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
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
    public static ConfigOption<Boolean> INVENTORY_HANDLING;
    public static ConfigOption<Boolean> PERMISSIONS;
    public static ConfigOption<Boolean> FOCUS_MODE;
    public static ConfigOption<List<String>> FOCUS_MODE_WHITELIST;
    public static ConfigOption<Boolean> GO_BACK;
    public static ConfigOption<Boolean> BUNGEECORD;

    public static List<Integer> POSSIBLE_LEADS;
    public static ConfigOption<Boolean> VERBOSE;
    public static ConfigOption<Boolean> GAMELOGS;

    public static ConfigOption<Boolean> UPDATER;

    // Advanced settings
    public static ConfigOption<Direction> HEADING;

    public static ConfigOption<List<String>> LANGUAGES;
    public static ConfigOption<String> DEFAULT_LANG;
    public static ConfigOption<Boolean> JOIN_LEAVE;

    public static ConfigOption<String> DEFAULT_STYLE;
    public static ConfigOption<List<String>> STYLES;

    public static ConfigOption<Boolean> JOINING;
    public static ConfigOption<Boolean> PERMISSIONS_STYLES;
    public static ConfigOption<Boolean> SAVE_STATS;
    public static ConfigOption<Boolean> LEAVE_REWARDS;
    public static ConfigOption<Boolean> OPTIONS_ENABLED;
    public static ConfigOption<Boolean> HEALTH_HANDLING;
    public static ConfigOption<Boolean> INVENTORY_SAVING;
    public static ConfigOption<String> ALT_INVENTORY_SAVING_COMMAND;

    public static ConfigOption<Integer> OPTIONS_TIME_FORMAT;

    public static HashMap<String, String> OPTIONS_DEFAULTS;
    public static ConfigOption<Boolean> HOTBAR_QUIT_ITEM;

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

        OPTIONS_TIME_FORMAT = new ConfigOption<>(config, "options.time.format");

        VERBOSE = new ConfigOption<>(config, "verbose");
        HEALTH_HANDLING = new ConfigOption<>(config, "options.health-handling");
        INVENTORY_SAVING = new ConfigOption<>(config, "options.inventory-saving");
        ALT_INVENTORY_SAVING_COMMAND = new ConfigOption<>(config, "options.alt-inventory-saving-command");

        // todo fix this
        List<String> options = Arrays.asList("leads", "time", "difficulty", "schematic-difficulty", "adaptive-difficulty", "particles", "scoreboard", "death-msg", "special", "structure");
        OPTIONS_DEFAULTS = new HashMap<>();
        for (String node : Util.getNode(items, "items.options")) {
            for (String option : options) {
                if (option.equalsIgnoreCase(node)) {
                    String value = items.getString("items.options." + node + ".default");
                    if (value == null) {
                        Logging.stack("Default option '" + node + "' is null!", "Please check your items.yml file and check the default options!");
                        continue;
                    }
                    OPTIONS_DEFAULTS.put(node, value);
                }
            }
        }

        HOTBAR_QUIT_ITEM = new ConfigOption<>(config, "options.hotbar-quit-item");

        JOIN_LEAVE = new ConfigOption<>(lang, "options.join-leave-enabled");
        HEADING = new ConfigOption<>(Util.getDirection(generation.getString("advanced.island.parkour.heading")));
        PERMISSIONS_STYLES = new ConfigOption<>(config, "permissions.per-style");
        GAMELOGS = new ConfigOption<>(config, "sql.game-logs");
        LANGUAGES = new ConfigOption<>(new ArrayList<>(lang.getConfigurationSection("messages").getKeys(false)));
        List<String> languages = new ArrayList<>(LANGUAGES.get());
        languages.remove("default");
        LANGUAGES.thenSet(languages);
        DEFAULT_LANG = new ConfigOption<>(lang, "messages.default");

        DEFAULT_STYLE = new ConfigOption<>(config, "styles.default");
        STYLES = new ConfigOption<>(Util.getNode(config, "styles.list"));

        SAVE_STATS = new ConfigOption<>(config, "options.save-stats");
        OPTIONS_ENABLED = new ConfigOption<>(config, "options.enabled");

        UPDATER = new ConfigOption<>(config, "update-checker");
        JOINING = new ConfigOption<>(config, "joining");

        // Config stuff

        POSSIBLE_LEADS = config.getIntegerList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1) {
                Logging.error("Invalid lead in config: found " + lead + ", should be >1");
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        INVENTORY_HANDLING = new ConfigOption<>(config, "options.inventory-handling");
        PERMISSIONS = new ConfigOption<>(config, "permissions.enabled");
        FOCUS_MODE = new ConfigOption<>(config, "focus-mode.enabled");
        FOCUS_MODE_WHITELIST = new ConfigOption<>(config, "focus-mode.whitelist");

        GO_BACK = new ConfigOption<>(config, "bungeecord.go-back-enabled");

        if (init) {
            BORDER_SIZE =  new ConfigOption<>(generation, "advanced.border-size");
            SQL = new ConfigOption<>(config, "sql.enabled");
        }

        SchematicCache.read();
    }

    public static ConfigOption<String> PARTICLE_SHAPE;
    public static ConfigOption<Sound> SOUND_TYPE;
    public static ConfigOption<Integer> SOUND_PITCH;
    public static ConfigOption<Particle> PARTICLE_TYPE;

    // Very not efficient but this is basically the only way to ensure the enums have a value
    private static void initEnums() {
        String enumValue;
        enumValue = config.getString("particles.sound-type").toUpperCase();

        if (Version.isHigherOrEqual(Version.V1_9)) { // 1.8 has no Particle class & severely limited Sound support
            try {
                SOUND_TYPE = new ConfigOption<>(Sound.valueOf(enumValue));
            } catch (IllegalArgumentException ex) {
                try {
                    SOUND_TYPE = new ConfigOption<>(Sound.valueOf("BLOCK_NOTE_PLING"));
                } catch (IllegalArgumentException ex2) {
                    Logging.error("Invalid sound: " + enumValue);
                    SOUND_TYPE = new ConfigOption<>(Sound.values()[0]);
                }
            }
            SOUND_PITCH = new ConfigOption<>(config, "particles.sound-pitch");

            try {
                PARTICLE_TYPE = new ConfigOption<>(Particle.valueOf(enumValue));
            } catch (IllegalArgumentException ex) {
                try {
                    PARTICLE_TYPE = new ConfigOption<>(Particle.valueOf("SPELL_INSTANT"));
                } catch (IllegalArgumentException ex2) {
                    Logging.error("Invalid particle: " + enumValue);
                    PARTICLE_TYPE = new ConfigOption<>(Particle.values()[0]);
                }
            }

            PARTICLE_SHAPE = new ConfigOption<>(config, "particles.particle-shape");
        }
    }


    // --------------------------------------------------------------
    // MySQL
    public static ConfigOption<Boolean> SQL;
    public static ConfigOption<Integer> SQL_PORT;
    public static ConfigOption<String> SQL_URL;
    public static ConfigOption<String> SQL_DB;
    public static ConfigOption<String> SQL_USERNAME;
    public static ConfigOption<String> SQL_PASSWORD;
    public static ConfigOption<String> SQL_PREFIX;

    private static void initSql() {
        SQL_PORT = new ConfigOption<>(config, "sql.port");
        SQL_DB = new ConfigOption<>(config, "sql.database");
        SQL_URL = new ConfigOption<>(config, "sql.url");
        SQL_USERNAME = new ConfigOption<>(config, "sql.username");
        SQL_PASSWORD = new ConfigOption<>(config, "sql.password");
        SQL_PREFIX = new ConfigOption<>(config, "sql.prefix");
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

    public static ConfigOption<Integer> MAX_Y;
    public static ConfigOption<Integer> MIN_Y;

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

        MAX_Y = new ConfigOption<>(generation, "generation.settings.max-y");
        MIN_Y = new ConfigOption<>(generation, "generation.settings.min-y");
    }

    // --------------------------------------------------------------
    // Advanced settings in generation

    public static ConfigOption<Integer> BORDER_SIZE;
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
    // Rewards

    public static ConfigOption<Boolean> ALL_POINTS;
    public static ConfigOption<Boolean> REWARDS;
    public static HashMap<Integer, List<String>> REWARDS_SCORES;
    public static HashMap<Integer, List<String>> ON_LEAVE_REWARDS_SCORES;
    public static List<String> INTERVAL_REWARDS_SCORES;
    public static ConfigOption<Integer> REWARDS_INTERVAL;
    public static ConfigOption<Double> REWARDS_MONEY;
    public static ConfigOption<List<String>> REWARDS_COMMANDS;
    public static ConfigOption<String> REWARDS_MESSAGE;

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

        BUNGEECORD = new ConfigOption<>(config, "bungeecord.enabled");
        REWARDS = new ConfigOption<>(config, "rewards.enabled");
        REWARDS_MONEY = new ConfigOption<>(config, "rewards.vault-reward");
        REWARDS_INTERVAL = new ConfigOption<>(config, "rewards.interval");
        REWARDS_COMMANDS = new ConfigOption<>(config, "rewards.command");
        REWARDS_MESSAGE = new ConfigOption<>(config, "rewards.message");
        if (REWARDS_MESSAGE.get().equalsIgnoreCase("null") || REWARDS_MESSAGE.get().equals("''") || REWARDS_MESSAGE.get().equals("")) {
            REWARDS_MESSAGE = null;
        }

        LEAVE_REWARDS = new ConfigOption<>(config, "rewards.leave-rewards");
        ALL_POINTS = new ConfigOption<>(config, "scoring.all-points");
    }

    // --------------------------------------------------------------
    // Scoreboard

    public static ConfigOption<Boolean> SCOREBOARD;
    public static ConfigOption<String> SCOREBOARD_TITLE;
    public static List<String> SCOREBOARD_LINES;

    private static void initScoreboard() {
        SCOREBOARD = new ConfigOption<>(lang, "scoreboard.enabled");
        SCOREBOARD_TITLE = new ConfigOption<>(lang, "scoreboard.title");
        SCOREBOARD_LINES = Util.colorList(lang.getStringList("scoreboard.lines"));
    }

    public enum ParticleShape {
        DOT,
        CIRCLE,
        BOX
    }
}