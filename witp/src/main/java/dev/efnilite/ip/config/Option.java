package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.style.Style;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.particle.ParticleData;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiFunction;

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
    public static boolean ON_JOIN;

    public static List<Integer> POSSIBLE_LEADS;

    // Advanced settings
    public static Vector HEADING;
    public static boolean JOINING;
    public static boolean PERMISSIONS_STYLES;
    public static boolean HEALTH_HANDLING;
    public static boolean INVENTORY_SAVING;
    public static String ALT_INVENTORY_SAVING_COMMAND;

    public static int OPTIONS_TIME_FORMAT;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String> OPTIONS_DEFAULTS;

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
        initStyles("styles.list", "default", Config.CONFIG.fileConfiguration, (materials, session) -> Colls.random(materials));

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

        if (!WORLD_NAME.matches("[a-zA-Z0-9_-]+")) {
            IP.logging().stack("Invalid world name: %s".formatted(WORLD_NAME), "world names need to contain only a-z, A-Z, 0-9, _ or -.");

            WORLD_NAME = "witp";
        }

        // Options

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
            String parent = "%s.%s".formatted(prefix, option.path);

            // register enabled value
            OPTIONS_ENABLED.put(option, Config.CONFIG.getBoolean("%s.enabled".formatted(parent)));

            // register default value
            if (!Config.CONFIG.isPath("%s.default".formatted(parent))) {
                continue;
            }

            Object value = Config.CONFIG.get("%s.default".formatted(parent));

            if (value != null) {
                OPTIONS_DEFAULTS.put(option, String.valueOf(value));
            }
        }

        // =====================================

        PERMISSIONS_STYLES = Config.CONFIG.getBoolean("permissions.per-style");

        // Config stuff

        POSSIBLE_LEADS = Config.CONFIG.getIntList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1 || lead > 128) {
                IP.logging().error("Invalid lead: %d. Should be above 1 and below 128.".formatted(lead));
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        INVENTORY_HANDLING = Config.CONFIG.getBoolean("options.inventory-handling");
        PERMISSIONS = Config.CONFIG.getBoolean("permissions.enabled");
        FOCUS_MODE = Config.CONFIG.getBoolean("focus-mode.enabled");
        FOCUS_MODE_WHITELIST = Config.CONFIG.getStringList("focus-mode.whitelist");

        // Bungeecord
        GO_BACK = Config.CONFIG.getBoolean("bungeecord.go-back-enabled");
        ON_JOIN = Config.CONFIG.getBoolean("bungeecord.enabled");

        // Generation
        HEADING = stringToVector(Config.GENERATION.getString("advanced.island.parkour.heading"));

        // Scoring
        ALL_POINTS = Config.CONFIG.getBoolean("scoring.all-points");
        REWARDS_USE_TOTAL_SCORE = Config.CONFIG.getBoolean("scoring.rewards-use-total-score");

        if (firstLoad) {
            BORDER_SIZE = Config.GENERATION.getDouble("advanced.border-size");
            SQL = Config.CONFIG.getBoolean("sql.enabled");
        }
    }

    private static Vector stringToVector(String direction) {
        return switch (direction.toLowerCase()) {
            case "north" -> new org.bukkit.util.Vector(0, 0, -1);
            case "south" -> new org.bukkit.util.Vector(0, 0, 1);
            case "west" -> new org.bukkit.util.Vector(-1, 0, 0);
            default -> new Vector(1, 0, 0); // east
        };
    }

    private static Location parseLocation(String location) {
        String[] values = location.replaceAll("[()]", "").replaceAll("[, ]", " ").split(" ");

        World world = Bukkit.getWorld(values[3]);

        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        return new Location(world, Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
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
            SOUND_TYPE = Sound.valueOf("BLOCK_NOTE_PLING");
            IP.logging().error("Invalid sound: %s".formatted(value));
        }

        value = Config.CONFIG.getString("particles.particle-type");
        try {
            PARTICLE_TYPE = Particle.valueOf(value);
        } catch (IllegalArgumentException ex) {
            PARTICLE_TYPE = Particle.valueOf("SPELL_INSTANT");
            IP.logging().error("Invalid particle type: %s".formatted(value));
        }

        SOUND_PITCH = Config.CONFIG.getInt("particles.sound-pitch");
        PARTICLE_SHAPE = ParticleShape.valueOf(Config.CONFIG.getString("particles.particle-shape").toUpperCase());
        PARTICLE_DATA = new ParticleData<>(PARTICLE_TYPE, null, 10, 0, 0, 0, 0);
    }

    public enum ParticleShape {
        DOT, CIRCLE, BOX
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

    public static double TYPE_NORMAL;
    public static double TYPE_SPECIAL;
    public static double TYPE_SCHEMATICS;

    public static double SPECIAL_ICE;
    public static double SPECIAL_SLAB;
    public static double SPECIAL_PANE;
    public static double SPECIAL_FENCE;

    public static double NORMAL_DISTANCE_1;
    public static double NORMAL_DISTANCE_2;
    public static double NORMAL_DISTANCE_3;
    public static double NORMAL_DISTANCE_4;

    public static double NORMAL_HEIGHT_1;
    public static double NORMAL_HEIGHT_0;
    public static double NORMAL_HEIGHT_NEG1;
    public static double NORMAL_HEIGHT_NEG2;

    public static int MAX_Y;
    public static int MIN_Y;

    private static void initGeneration() {
        TYPE_NORMAL = Config.GENERATION.getInt("generation.type.normal") / 100.0;
        TYPE_SPECIAL = Config.GENERATION.getInt("generation.type.schematic") / 100.0;
        TYPE_SCHEMATICS = Config.GENERATION.getInt("generation.type.special") / 100.0;

        SPECIAL_ICE = Config.GENERATION.getInt("generation.special.ice") / 100.0;
        SPECIAL_SLAB = Config.GENERATION.getInt("generation.special.slab") / 100.0;
        SPECIAL_PANE = Config.GENERATION.getInt("generation.special.pane") / 100.0;
        SPECIAL_FENCE = Config.GENERATION.getInt("generation.special.fence") / 100.0;

        NORMAL_DISTANCE_1 = Config.GENERATION.getInt("generation.normal.distance.1") / 100.0;
        NORMAL_DISTANCE_2 = Config.GENERATION.getInt("generation.normal.distance.2") / 100.0;
        NORMAL_DISTANCE_3 = Config.GENERATION.getInt("generation.normal.distance.3") / 100.0;
        NORMAL_DISTANCE_4 = Config.GENERATION.getInt("generation.normal.distance.4") / 100.0;

        NORMAL_HEIGHT_1 = Config.GENERATION.getInt("generation.normal.height.1") / 100.0;
        NORMAL_HEIGHT_0 = Config.GENERATION.getInt("generation.normal.height.0") / 100.0;
        NORMAL_HEIGHT_NEG1 = Config.GENERATION.getInt("generation.normal.height.-1") / 100.0;
        NORMAL_HEIGHT_NEG2 = Config.GENERATION.getInt("generation.normal.height.-2") / 100.0;

        MAX_Y = Config.GENERATION.getInt("generation.settings.max-y");
        MIN_Y = Config.GENERATION.getInt("generation.settings.min-y");

        if (MIN_Y >= MAX_Y) {
            MIN_Y = 100;
            MAX_Y = 200;

            IP.logging().stack("Provided minimum y is the same or larger than maximum y!", "check your generation.yml file");
        }
    }

    // --------------------------------------------------------------
    // Advanced settings in generation

    public static double BORDER_SIZE;
    public static int GENERATOR_CHECK;
    public static int SCHEMATIC_COOLDOWN;

    private static void initAdvancedGeneration() {
        GENERATOR_CHECK = Config.GENERATION.getInt("advanced.generator-check");

        SCHEMATIC_COOLDOWN = Config.GENERATION.getInt("advanced.schematic-cooldown");
    }

    // --------------------------------------------------------------

    public static void initStyles(String path, String type, FileConfiguration config, BiFunction<List<BlockData>, Session, BlockData> materialSelector) {
        for (String style : Util.getChildren(config, path, false)) {
            Registry.register(new Style(
                style,
                config.getStringList("%s.%s".formatted(path, style)).stream()
                    .map(name -> {
                        Material material = Material.getMaterial(name.toUpperCase());

                        if (material == null) {
                            IP.logging().warn("Unknown material %s in style %s".formatted(name, style));
                            return Material.SMOOTH_QUARTZ.createBlockData();
                        }

                        return material.createBlockData();
                    })
                    .toList(),
                type,
                materialSelector));
        }
    }
}