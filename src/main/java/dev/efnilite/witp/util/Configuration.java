package dev.efnilite.witp.util;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * An utilities class for the Configuration
 */
public class Configuration {

    private final Plugin plugin;
    private final String[] defaultFiles;
    private final HashMap<String, FileConfiguration> files;

    /**
     * Create a new instance
     */
    public Configuration(Plugin plugin) {
        this.plugin = plugin;
        files = new HashMap<>();

        defaultFiles = new String[] {"config.yml", "generation.yml", "lang.yml"};

        if (!new File(plugin.getDataFolder() + "/lang.yml").exists()) {
            plugin.getDataFolder().mkdirs();

            for (String file : defaultFiles) {
                plugin.saveResource(file, false);
            }
            Verbose.info("Downloaded all config files");
        }
        for (String file : defaultFiles) {
            files.put(file.replaceAll("(.+/|.yml)", ""), this.getFile(plugin.getDataFolder() + "/" + file));
        }

        structures();
    }

    public void reload() {
        files.put("lang", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang.yml")));
    }

    /**
     * Downloads the structures
     */
    private void structures() {
        if (!(new File(plugin.getDataFolder().toString() + "/structures/parkour-1.nbt").exists())) {
            String[] schematics = new String[] {"spawn-island.nbt"};
            File folder = new File(plugin.getDataFolder().toString() + "/structures");
            folder.mkdirs();
            Verbose.info("Downloading all structures...");
            int structureCount = 18;
            BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                    try {
                        for (String schematic : schematics) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/structures/" + schematic).openStream();
                            Files.copy(stream, Paths.get(folder + "/" + schematic));
                            Verbose.verbose("Downloaded " + schematic);
                            stream.close();
                        }
                        for (int i = 1; i <= structureCount; i++) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/structures/parkour-" + i + ".nbt").openStream();
                            Files.copy(stream, Paths.get(folder + "/parkour-" + i + ".nbt"));
                            Verbose.verbose("Downloaded parkour-" + i);
                            stream.close();
                        }
                        Verbose.info("Downloaded all structures");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Verbose.error("Stopped download - please delete all the structures that have been downloaded and restart the server");
                    }
                    this.cancel();
                }
            };
            Tasks.asyncTask(runnable);
        }
    }

    /**
     * Get a file
     */
    public FileConfiguration getFile(String file) {
        FileConfiguration config;
        if (files.get(file) == null) {
            config = YamlConfiguration.loadConfiguration(new File(file));
            files.put(file, config);
        } else {
            config = files.get(file);
        }
        return config;
    }

    /**
     * Gets a coloured string
     *
     * @param   file
     *          The file
     * @param   path
     *          The path
     *
     * @return a coloured string
     */
    public @Nullable List<String> getStringList(String file, String path) {
        List<String> string = this.getFile(file).getStringList(path);
        if (string.size() == 0) {
            return null;
        }
        return Util.color(string);
    }

    /**
     * Gets a coloured string
     *
     * @param   file
     *          The file
     * @param   path
     *          The path
     *
     * @return a coloured string
     */
    public @Nullable String getString(String file, String path) {
        String string = this.getFile(file).getString(path);
        if (string == null) {
            return null;
        }
        return Util.color(string);
    }

    /**
     * Class for variables required in generating without accessing the file a lot (constants)
     */
    public static class Option {

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
        public static int REWARDS_INTERVAL;
        public static int REWARDS_SCORE;
        public static double REWARDS_MONEY;
        public static String REWARDS_COMMAND;
        public static String REWARDS_MESSAGE;

        public static Sound SOUND_TYPE;
        public static int SOUND_PITCH;
        public static Particle PARTICLE_TYPE;

        public static boolean SCOREBOARD;
        public static boolean INVENTORY_HANDLING;
        public static String SCOREBOARD_TITLE;
        public static List<String> SCOREBOARD_LINES;
        public static boolean PERMISSIONS;
        public static boolean FOCUS_MODE;
        public static boolean GO_BACK;
        public static Location GO_BACK_LOC;

        // Advanced settings
        public static double BORDER_SIZE;
        public static int GENERATOR_CHECK;
        public static double HEIGHT_GAP;
        public static double MULTIPLIER;

        public static int MAXED_ONE_BLOCK;
        public static int MAXED_TWO_BLOCK;
        public static int MAXED_THREE_BLOCK;
        public static int MAXED_FOUR_BLOCK;

        public static void init(boolean init) {
            FileConfiguration gen = WITP.getConfiguration().getFile("generation");
            FileConfiguration config = WITP.getConfiguration().getFile("config");
            FileConfiguration lang = WITP.getConfiguration().getFile("lang");

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
            REWARDS = config.getBoolean("rewards.enabled");
            REWARDS_INTERVAL = config.getInt("rewards.interval");
            REWARDS_MONEY = config.getInt("rewards.vault-reward");
            REWARDS_SCORE = config.getInt("rewards.score");
            REWARDS_COMMAND = config.getString("rewards.command").replaceAll("/", "");
            if (REWARDS_COMMAND.equalsIgnoreCase("null")) {
                REWARDS_COMMAND = null;
            }
            REWARDS_MESSAGE = config.getString("rewards.message");
            if (REWARDS_MESSAGE.equalsIgnoreCase("null")) {
                REWARDS_MESSAGE = null;
            }

            SCOREBOARD = lang.getBoolean("scoreboard.enabled");
            SCOREBOARD_TITLE = Util.color(lang.getString("scoreboard.title"));
            SCOREBOARD_LINES = Util.color(lang.getStringList("scoreboard.lines"));
            INVENTORY_HANDLING = config.getBoolean("options.inventory-handling");
            PERMISSIONS = config.getBoolean("permissions.enabled");
            FOCUS_MODE = config.getBoolean("focus-mode.enabled");

            GO_BACK = config.getBoolean("bungeecord.go-back-enabled");
            GO_BACK_LOC = Util.parseLocation(config.getString("bungeecord.go-back"));

            SOUND_TYPE = Sound.valueOf(config.getString("particles.sound-type").toUpperCase());
            SOUND_PITCH = config.getInt("particles.sound-pitch");
            PARTICLE_TYPE = Particle.valueOf(config.getString("particles.particle-type").toUpperCase());

            // Advanced settings
            if (init) {
                BORDER_SIZE = gen.getDouble("advanced.border-size");
            }
            GENERATOR_CHECK = gen.getInt("advanced.generator-check");
            HEIGHT_GAP = gen.getDouble("advanced.height-gap");
            MULTIPLIER = gen.getInt("advanced.maxed-multiplier");

            MAXED_ONE_BLOCK = gen.getInt("advanced.maxed-values.1-block");
            MAXED_TWO_BLOCK = gen.getInt("advanced.maxed-values.2-block");
            MAXED_THREE_BLOCK = gen.getInt("advanced.maxed-values.3-block");
            MAXED_FOUR_BLOCK = gen.getInt("advanced.maxed-values.4-block");
        }
    }
}