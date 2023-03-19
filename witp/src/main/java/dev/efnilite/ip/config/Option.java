package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.vilib.particle.ParticleData;
import org.bukkit.*;

import java.util.*;

/**
 * Class for variables required in generating without accessing the file a lot (constants)
 */
public class Option {

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
    public static boolean JOINING;
    public static boolean PERMISSIONS_STYLES;
    public static boolean SETTINGS_ENABLED;
    public static boolean HEALTH_HANDLING;
    public static boolean INVENTORY_SAVING;
    public static String ALT_INVENTORY_SAVING_COMMAND;

    public static int OPTIONS_TIME_FORMAT;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, Object> OPTIONS_DEFAULTS;

    // Worlds
    public static boolean DELETE_ON_RELOAD;
    public static String WORLD_NAME;

    public static Location GO_BACK_LOC;

    public static int STORAGE_UPDATE_INTERVAL = 30;

    public static void init(boolean firstLoad) {
        initSql();
        initEnums();
        initGeneration();
        initAdvancedGeneration();

        STORAGE_UPDATE_INTERVAL = Config.CONFIG.getInt("storage-update-interval");

        GO_BACK_LOC = parseLocation(Config.CONFIG.getString("bungeecord.go-back"));
        String[] axes = Config.CONFIG.getString("bungeecord.go-back-axes").split(",");
        GO_BACK_LOC.setPitch(Float.parseFloat(axes[0]));
        GO_BACK_LOC.setYaw(Float.parseFloat(axes[1]));

        // General settings
        AUTO_UPDATER = Config.CONFIG.getBoolean("auto-updater");
        JOINING = Config.CONFIG.getBoolean("joining");

        // Worlds
        DELETE_ON_RELOAD = Config.CONFIG.getBoolean("world.delete-on-reload");
        WORLD_NAME = Config.CONFIG.getString("world.name");

        if (!WORLD_NAME.matches("[a-zA-Z0-9/._-]+")) {
            IP.logging().stack("Invalid world name!", "world names need to match regex \"[a-zA-Z0-9/._-]+\"");
        }

        // Options

        SETTINGS_ENABLED = Config.CONFIG.getBoolean("options.enabled");
        OPTIONS_TIME_FORMAT = Config.CONFIG.getInt("options.time.format");
        HEALTH_HANDLING = Config.CONFIG.getBoolean("options.health-handling");
        INVENTORY_SAVING = Config.CONFIG.getBoolean("options.inventory-saving");
        ALT_INVENTORY_SAVING_COMMAND = Config.CONFIG.getString("options.alt-inventory-saving-command");

        List<ParkourOption> options = new ArrayList<>(Arrays.asList(ParkourOption.values()));

        // exceptions
        options.remove(ParkourOption.JOIN);
        options.remove(ParkourOption.ADMIN);

        // =====================================

        OPTIONS_DEFAULTS = new HashMap<>();
        OPTIONS_ENABLED = new HashMap<>();

        String prefix = "default-values";
        for (ParkourOption option : options) {
            String path = prefix + "." + option.getPath();

            // register default value
            Object value = Config.CONFIG.get(path + ".default");

            if (value != null) {
                OPTIONS_DEFAULTS.put(option, value);
            }

            // register enabled value
            boolean enabled = true;

            if (Config.CONFIG.isPath("%s.enabled".formatted(path))) {
                enabled = Config.CONFIG.getBoolean("%s.enabled".formatted(path));
            }

            OPTIONS_ENABLED.put(option, enabled);
        }

        // =====================================

        PERMISSIONS_STYLES = Config.CONFIG.getBoolean("permissions.per-style");

        // Config stuff

        POSSIBLE_LEADS = Config.CONFIG.getIntList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1 || lead > 128) {
                IP.logging().error("Invalid lead in config: found " + lead + ", should be above 1 and below 128 to prevent lag on spawn.");
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        INVENTORY_HANDLING = Config.CONFIG.getBoolean("options.inventory-handling");
        PERMISSIONS = Config.CONFIG.getBoolean("permissions.enabled");
        FOCUS_MODE = Config.CONFIG.getBoolean("focus-mode.enabled");
        FOCUS_MODE_WHITELIST = Config.CONFIG.getStringList("focus-mode.whitelist");

        // Bungeecord
        GO_BACK = Config.CONFIG.getBoolean("bungeecord.go-back-enabled");
        BUNGEECORD = Config.CONFIG.getBoolean("bungeecord.enabled");

        // Generation
        HEADING = Config.GENERATION.getString("advanced.island.parkour.heading");

        // Scoring
        ALL_POINTS = Config.CONFIG.getBoolean("scoring.all-points");
        REWARDS_USE_TOTAL_SCORE = Config.CONFIG.getBoolean("scoring.rewards-use-total-score");

        if (firstLoad) {
            BORDER_SIZE = Config.GENERATION.getDouble("advanced.border-size");
            SQL = Config.CONFIG.getBoolean("sql.enabled");
        }
    }

    private static Location parseLocation(String location) {
        String[] values = location.replaceAll("[()]", "").replace(", ", " ").replace(",", " ").split(" ");
        World world = Bukkit.getWorld(values[3]);
        if (world == null) {
            IP.logging().error("Detected an invalid world: " + values[3]);
            return new Location(Bukkit.getWorlds().get(0), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
        }
        return new Location(Bukkit.getWorld(values[3]), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
    }

    public static ParticleShape PARTICLE_SHAPE;
    public static Sound SOUND_TYPE;
    public static int SOUND_PITCH;
    public static Particle PARTICLE_TYPE;
    public static ParticleData<?> PARTICLE_DATA;

    private static void initEnums() {
        String value = Config.CONFIG.getString("particles.sound-type").toUpperCase();

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

        value = Config.CONFIG.getString("particles.particle-type");
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

        SOUND_PITCH = Config.CONFIG.getInt("particles.sound-pitch");
        PARTICLE_SHAPE = ParticleShape.valueOf(Config.CONFIG.getString("particles.particle-shape").toUpperCase());
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
        SQL_PORT = Config.CONFIG.getInt("sql.port");
        SQL_DB = Config.CONFIG.getString("sql.database");
        SQL_URL = Config.CONFIG.getString("sql.url");
        SQL_USERNAME = Config.CONFIG.getString("sql.username");
        SQL_PASSWORD = Config.CONFIG.getString("sql.password");
        SQL_PREFIX = Config.CONFIG.getString("sql.prefix");
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
        NORMAL = Config.GENERATION.getInt("generation.normal-jump.chance");
        SCHEMATICS = Config.GENERATION.getInt("generation.structures.chance");
        SPECIAL = Config.GENERATION.getInt("generation.normal-jump.special.chance");

        SPECIAL_ICE = Config.GENERATION.getInt("generation.normal-jump.special.ice");
        SPECIAL_SLAB = Config.GENERATION.getInt("generation.normal-jump.special.slab");
        SPECIAL_PANE = Config.GENERATION.getInt("generation.normal-jump.special.pane");
        SPECIAL_FENCE = Config.GENERATION.getInt("generation.normal-jump.special.fence");

        NORMAL_ONE_BLOCK = Config.GENERATION.getInt("generation.normal-jump.1-block");
        NORMAL_TWO_BLOCK = Config.GENERATION.getInt("generation.normal-jump.2-block");
        NORMAL_THREE_BLOCK = Config.GENERATION.getInt("generation.normal-jump.3-block");
        NORMAL_FOUR_BLOCK = Config.GENERATION.getInt("generation.normal-jump.4-block");

        NORMAL_UP = Config.GENERATION.getInt("generation.normal-jump.up");
        NORMAL_LEVEL = Config.GENERATION.getInt("generation.normal-jump.level");
        NORMAL_DOWN = Config.GENERATION.getInt("generation.normal-jump.down");
        NORMAL_DOWN2 = Config.GENERATION.getInt("generation.normal-jump.down2");

        MAX_Y = Config.GENERATION.getInt("generation.settings.max-y");
        MIN_Y = Config.GENERATION.getInt("generation.settings.min-y");

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

    public static int SCHEMATIC_COOLDOWN;

    private static void initAdvancedGeneration() {
        GENERATOR_CHECK = Config.GENERATION.getInt("advanced.generator-check");
        HEIGHT_GAP = Config.GENERATION.getDouble("advanced.height-gap");
        MULTIPLIER = Config.GENERATION.getDouble("advanced.maxed-multiplier");

        MAXED_ONE_BLOCK = Config.GENERATION.getInt("advanced.maxed-values.1-block");
        MAXED_TWO_BLOCK = Config.GENERATION.getInt("advanced.maxed-values.2-block");
        MAXED_THREE_BLOCK = Config.GENERATION.getInt("advanced.maxed-values.3-block");
        MAXED_FOUR_BLOCK = Config.GENERATION.getInt("advanced.maxed-values.4-block");
        SCHEMATIC_COOLDOWN = Config.GENERATION.getInt("advanced.schematic-cooldown");
    }

    public enum ParticleShape {
        DOT,
        CIRCLE,
        BOX
    }
}