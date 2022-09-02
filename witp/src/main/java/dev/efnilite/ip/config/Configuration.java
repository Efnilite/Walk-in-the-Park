package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.legacy.LegacyLeaderboardData;
import dev.efnilite.ip.reward.RewardReader;
import dev.efnilite.ip.schematic.SchematicCache;
import dev.efnilite.vilib.lib.configupdater.configupdater.ConfigUpdater;
import dev.efnilite.vilib.util.Task;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * An utilities class for the Configuration
 */
public class Configuration {

    private final Plugin plugin;
    private final Map<String, FileConfiguration> files = new HashMap<>();

    /**
     * Create a new instance
     */
    public Configuration(Plugin plugin) {
        this.plugin = plugin;

        List<String> defaultFiles = Arrays.asList("config.yml", "rewards-v2.yml", "generation.yml", "schematics.yml");
        for (String name : defaultFiles) {
            File file = new File(plugin.getDataFolder(), name);

            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();

                plugin.saveResource(name, false);
                IP.logging().info("Created config file " + name);
            }
        }

        // for versions before the leaderboard update v3.6.0
        YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        if (c.contains("enable-joining")) {
            Task.create(plugin)
                    .async()
                    .delay(10 * 20)
                    .execute(LegacyLeaderboardData::migrate)
                    .run();
        }

        // Config files without languages
        try {
            ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"), List.of("styles"));
            ConfigUpdater.update(plugin, "generation.yml", new File(plugin.getDataFolder(), "generation.yml"), new ArrayList<>());
            ConfigUpdater.update(plugin, "schematics.yml", new File(plugin.getDataFolder(), "schematics.yml"), List.of("difficulty"));

        } catch (IOException ex) {
            IP.logging().stack("Error while trying to update a config file",
                    "delete all config files and restart the server", ex);
        }

        reload();

        IP.logging().info("Loaded all config files");
    }

    /**
     * Maps all files to aliases.
     */
    public void reload() {
        files.put("lang", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/messages-v3.yml")));
        files.put("items", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/items-v3.yml")));
        files.put("scoreboard", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/scoreboard-v3.yml")));

        files.put("config", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/config.yml")));
        files.put("rewards", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/rewards-v2.yml")));
        files.put("generation", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/generation.yml")));
        files.put("schematics", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/schematics.yml")));

        // read rewards file
        RewardReader.readRewards(files.get("rewards"));

        if (schematics()) {
            // read schematics again
            SchematicCache.invalidate();
            SchematicCache.read();
        }
    }

    /**
     * Downloads the structures
     *
     * @return true if schematics are already found, false if not
     */
    private boolean schematics() {
        if (new File(plugin.getDataFolder() + "/schematics/spawn-island-duels.witp").exists()) {
            return true;
        }

        String[] schematics = new String[]{"spawn-island.witp", "spawn-island-duels.witp"};
        File folder = new File(plugin.getDataFolder(), "schematics");
        folder.mkdirs();

        IP.logging().info("Downloading missing schematics...");
        int structureCount = 21;

        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try {
                        for (String schematic : schematics) {
                            Path path = Paths.get(folder + "/" + schematic);
                            if (path.toFile().exists()) {
                                continue;
                            }

                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/" + schematic).openStream();
                            Files.copy(stream, path);
                            stream.close();
                        }
                        for (int i = 1; i <= structureCount; i++) {
                            Path path = Paths.get(folder + "/parkour-" + i + ".witp");
                            if (path.toFile().exists()) {
                                continue;
                            }

                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/parkour-" + i + ".witp").openStream();
                            Files.copy(stream, path);
                            stream.close();
                        }

                        SchematicCache.invalidate();
                        SchematicCache.read();
                        IP.logging().info("Downloaded all schematics");
                    } catch (FileAlreadyExistsException ex) {
                        // do nothing
                    } catch (UnknownHostException ex) {
                        IP.logging().stack("Stopped download of schematics",
                                "join the Discord and send this error to receive help", ex);
                    } catch (Throwable throwable) {
                        IP.logging().stack("Stopped download of schematics",
                                "delete the schematics folder and restart the server", throwable);
                    }
                })
                .run();
        return false;
    }

    /**
     * Gets a file
     *
     * @param   file
     *          The name of the file
     *
     * @return the FileConfiguration
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
     * Gets a coloured string that isn't null
     *
     * @param   file
     *          The file
     *
     * @param   path
     *          The path
     *
     * @return a coloured string that isn't null
     */
    public @NotNull String getString(@NotNull String file, @NotNull String path) {
        String string = getFile(file).getString(path);

        if (string == null) {
            IP.logging().stack("Option at path " + path + " with file " + file + " is null", "check the " + file + " file for misinputs");
            return "";
        }

        return string;
    }
}