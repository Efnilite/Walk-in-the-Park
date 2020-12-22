package dev.efnilite.witp.util;

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

        defaultFiles = new String[] {"config.yml", "generation.yml"};

        if (!new File(plugin.getDataFolder() + "/config.yml").exists()) {
            plugin.getDataFolder().mkdirs();

            for (String file : defaultFiles) {
                plugin.saveResource(file, false);
            }
            Verbose.info("Downloaded all config files");
        }

        for (String file : defaultFiles) {
            files.put(file.replaceAll("(.+/|.yml)", ""), this.getFile(plugin.getDataFolder() + "/" + file));
        }

//        structures();
//        BukkitRunnable runnable = new BukkitRunnable() {
//            @Override
//            public void run() {
//                save();
//            }
//        };
//        Tasks.asyncRepeat(runnable, 300 * 20);
    }

    /**
     * Saves the configuration files
     */
    public void save() {
        Verbose.verbose("Saving config files..");
        for (String file : defaultFiles) {
            try {
                getFile(file.replaceAll("(.+/|.yml)", "")).save(plugin.getDataFolder() + "/" + file);
            } catch (IOException e) {
                e.printStackTrace();
                Verbose.error("Couldn't init default files");
            }
        }
    }

    /**
     * Reloads the files
     */
    public void reload() {
        for (String file : new HashMap<>(files).keySet()) {
            String path = files.get(file).getCurrentPath();
            files.put(file, YamlConfiguration.loadConfiguration(new File(path)));
        }
    }

    /**
     * Downloads the structures
     */
    private void structures() {
        if (!(new File(plugin.getDataFolder().toString() + "/structures/zeus-4.nbt").exists())) {
            String[] schematics = new String[] {"archer-%l.nbt", "artillery-%l.nbt", "bee-%l.nbt", "force-%l.nbt", "ice-%l.nbt",
                    "leach-%l.nbt", "mage-%l.nbt", "necromancer-%l.nbt", "poison-%l.nbt", "quake-%l.nbt", "turret-%l.nbt", "zeus-%l.nbt"};
            File folder = new File(plugin.getDataFolder().toString() + "/structures");
            folder.mkdirs();
            Verbose.info("Downloading all structures...");
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        String replaced;
                        for (String schem : schematics) {
                            if (schem.contains("leach") || schem.contains("necromancer") || schem.contains("turret")) {
                                for (int i = 1; i < 4; i++) {
                                    replaced = schem.replaceAll("%l", Integer.toString(i));
                                    InputStream stream = new URL("https://github.com/Efnilite/TowerDefenceX/blob/master/structures/" + replaced + "?raw=true").openStream();
                                    Files.copy(stream, Paths.get(folder + "/" + replaced));
                                    Verbose.verbose("Downloaded " + replaced);
                                    stream.close();
                                }
                            }
                        }
                        Verbose.info("Downloaded all structures");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Verbose.error("Stopped download - delete all the structures that have been downloaded and restart the server");
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