package dev.efnilite.witp.util.config;

import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.task.Tasks;
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
import java.util.*;

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

        File folder = plugin.getDataFolder();
        if (!new File(folder, defaultFiles[0]).exists() || !new File(folder, defaultFiles[1]).exists() || !new File(folder, defaultFiles[2]).exists()) {
            plugin.getDataFolder().mkdirs();

            for (String file : defaultFiles) {
                plugin.saveResource(file, false);
            }
            Verbose.info("Downloaded all config files");
        }
        for (String file : defaultFiles) {
            try {
                ConfigUpdater.update(plugin, file, new File(plugin.getDataFolder(), file));
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to update config");
            }
            FileConfiguration configuration = this.getFile(folder + "/" + file);
//            configuration.options().copyDefaults(true);
            files.put(file.replaceAll("(.+/|.yml)", ""), configuration);
        }
        structures();
        Verbose.verbose("Loaded all config files");
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
}