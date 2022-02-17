package dev.efnilite.witp.util.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Task;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * An utilities class for the Configuration
 */
public class Configuration {

    private final Plugin plugin;
    private final HashMap<String, FileConfiguration> files;

    /**
     * Create a new instance
     */
    public Configuration(Plugin plugin) {
        this.plugin = plugin;
        files = new HashMap<>();

        String[] defaultFiles = new String[]{"config.yml", "generation.yml", "schematics.yml", "lang/messages-v3.yml", "lang/items-v3.yml", "lang/scoreboard-v3.yml"};

        File folder = plugin.getDataFolder();
        if (!new File(folder, defaultFiles[0]).exists() || !new File(folder, defaultFiles[1]).exists() || !new File(folder, defaultFiles[2]).exists() ||
                !new File(folder, defaultFiles[3]).exists() || !new File(folder, defaultFiles[4]).exists() || !new File(folder, defaultFiles[5]).exists()) {
            plugin.getDataFolder().mkdirs();

            for (String file : defaultFiles) {
                plugin.saveResource(file, false);
            }
            Logging.info("Downloaded all config files");
        }

        for (String file : defaultFiles) { // todo add for lang
            try {
                ConfigUpdater.update(plugin, file, new File(plugin.getDataFolder(), file), Collections.singletonList("styles"));
            } catch (IOException ex) {
                Logging.stack("Error while trying to update file " + file,
                        "Delete this file. If the problem persists, please report this error to the developer!", ex);
            }
        }
        reload();
        schematics();
        Logging.info("Loaded all config files");
    }

    public void reload() {
        files.put("lang", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/messages-v3.yml")));
        files.put("items", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/items-v3.yml")));
        files.put("scoreboard", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang/scoreboard-v3.yml")));

        files.put("config", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/config.yml")));
        files.put("generation", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/generation.yml")));
        files.put("schematics", YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/schematics.yml")));
    }

    /**
     * Downloads the structures
     */
    private void schematics() {
        if (new File(plugin.getDataFolder() + "/schematics/parkour-1.witp").exists() || !WITP.versionSupportsSchematics()) {
            return;
        }
        String[] schematics = new String[]{"spawn-island.witp"};
        File folder = new File(plugin.getDataFolder(), "schematics");
        folder.mkdirs();
        Logging.info("Downloading all schematics...");
        int structureCount = 21;

        new Task()
                .async()
                .execute(() -> {
                    try {
                        for (String schematic : schematics) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/" + schematic).openStream();
                            Files.copy(stream, Paths.get(folder + "/" + schematic));
                            Logging.verbose("Downloaded " + schematic);
                            stream.close();
                        }
                        for (int i = 1; i <= structureCount; i++) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/parkour-" + i + ".witp").openStream();
                            Files.copy(stream, Paths.get(folder + "/parkour-" + i + ".witp"));
                            Logging.verbose("Downloaded parkour-" + i);
                            stream.close();
                        }
                        SchematicCache.read();
                        Logging.info("Downloaded all schematics");
                    } catch (FileAlreadyExistsException ex) {
                        // do nothing
                    } catch (IOException ex) {
                        Logging.stack("Stopped download of schematics",
                                "Please delete all the structures that have been downloaded and restart the server", ex);
                    }
                })
                .run();
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
     * Gets a coloured string list. This list can't be null, only empty or containing items.
     *
     * @param   file
     *          The file
     * @param   path
     *          The path
     *
     * @return a coloured string
     */
    public @NotNull List<String> getStringList(@NotNull String file, @NotNull String path) {
        List<String> string = getFile(file).getStringList(path);
        if (string.isEmpty()) {
            return new ArrayList<>();
        }
        return Util.colorList(string);
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
            Logging.stack("Option at path " + path + " with file " + file + " is null", "Please check your config values");
            return "";
        }

        return Util.color(string);
    }

    /**
     * Gets a coloured string that may be null
     *
     * @param   file
     *          The file
     *
     * @param   path
     *          The path
     *
     * @return a coloured string
     */
    public @Nullable String getStringNullable(@NotNull String file, @NotNull String path, Runnable onNullFound) {
        String string = getFile(file).getString(path);
        if (string == null) {
            if (onNullFound != null) {
                onNullFound.run();
            }
            return null;
        }
        return Util.color(string);
    }

    /**
     * Gets an item from the items-v3.yml file and automatically creates it.
     *
     * @param   path
     *          The path of the item (excluding the parameters and 'items.')
     *
     * @param   replace
     *          What should be replaced in the lore/name
     *
     * @return the item based on the data from items-v3.yml
     */
    public Item getFromItemData(String locale, String path, @Nullable String... replace) {
        ItemData data = getItemData(path, locale, replace);
        return new Item(data.material, data.name).lore(data.lore);
    }

    private ItemData getItemData(String path, String locale, @Nullable String... replace) {
        FileConfiguration config = getFile("items");

        String namePath = "locale." + locale + "." + path;
        String matPath = "items." + path;

        String name = config.getString(namePath + ".name");
        if (name != null && replace != null && replace.length > 0) {
            name = name.replaceFirst("%[a-z]", replace[0]);
        }

        String l = config.getString(namePath + ".lore");
        List<String> lore = null;
        if (l != null) {
            lore = Arrays.asList(l.split("\\|\\|"));
            if (lore.size() != 0 && replace != null && replace.length > 0) {
                List<String> copy = new ArrayList<>();
                int index = 0;
                for (String s : lore) {
                    copy.add(s.replaceFirst("%[a-z]", replace[index]));
                }
                lore = copy;
            }
        }
        if (lore != null && lore.size() == 1 && lore.get(0).length() == 0) {
            lore = null;
        }

        Material material = null;
        String configMaterial = config.getString(matPath + ".item");
        if (configMaterial != null) {
            material = Material.getMaterial(configMaterial.toUpperCase());
        }

        return new ItemData(name, lore, material);
    }

    /**
     * Class to make gathering data (items-v3.yml) easier
     */
    private static class ItemData {

        public String name;
        public List<String> lore;
        public @Nullable Material material;

        public ItemData(String name, List<String> lore, @Nullable Material material) {
            this.name = name;
            this.lore = lore;
            this.material = material;
        }
    }
}