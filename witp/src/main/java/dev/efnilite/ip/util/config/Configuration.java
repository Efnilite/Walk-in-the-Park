package dev.efnilite.ip.util.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.legacy.LegacyLeaderboardData;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.reward.RewardReader;
import dev.efnilite.ip.schematic.SchematicCache;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
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
    private final Map<String, FileConfiguration> files = new HashMap<>();

    /**
     * Create a new instance
     */
    public Configuration(Plugin plugin) {
        this.plugin = plugin;

        List<String> defaultFiles = Arrays.asList("config.yml", "rewards-v2.yml", "generation.yml", "schematics.yml",
                "lang/messages-v3.yml", "lang/items-v3.yml", "lang/scoreboard-v3.yml");
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
            ConfigUpdater.update(plugin, "lang/scoreboard-v3.yml", new File(plugin.getDataFolder(), "lang/scoreboard-v3.yml"), new ArrayList<>());

//            ConfigUpdater.update(plugin, "lang/messages-v3.yml", new File(plugin.getDataFolder(), "lang/messages-v3.yml"), "messages");
//            ConfigUpdater.update(plugin, "lang/items-v3.yml", new File(plugin.getDataFolder(), "lang/items-v3.yml"), "locale");
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
        if (new File(plugin.getDataFolder() + "/schematics/parkour-1.witp").exists()) {
            return true;
        }
        String[] schematics = new String[]{"spawn-island.witp"};
        File folder = new File(plugin.getDataFolder(), "schematics");
        folder.mkdirs();
        IP.logging().info("Downloading all schematics...");
        int structureCount = 21;

        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    try {
                        for (String schematic : schematics) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/" + schematic).openStream();
                            Files.copy(stream, Paths.get(folder + "/" + schematic));
                            stream.close();
                        }
                        for (int i = 1; i <= structureCount; i++) {
                            InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/parkour-" + i + ".witp").openStream();
                            Files.copy(stream, Paths.get(folder + "/parkour-" + i + ".witp"));
                            stream.close();
                        }

                        SchematicCache.invalidate();
                        SchematicCache.read();
                        IP.logging().info("Downloaded all schematics");
                    } catch (FileAlreadyExistsException ex) {
                        // do nothing
                    } catch (IOException ex) {
                        IP.logging().stack("Stopped download of schematics",
                                "delete the schematics folder and restart the server", ex);
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
            IP.logging().stack("Option at path " + path + " with file " + file + " is null", "check the " + file + " file for misinputs");
            return "";
        }

        return Util.color(string);
    }

    public @NotNull String getLang(@NotNull String file, @NotNull String path) {
        String string = getFile(file).getString(path);

        if (string == null) {
            IP.logging().stack("Option at path " + path + " with file " + file + " is null", "check the " + file + " file for misinputs");
            return "";
        }

        return Util.color(string);
    }

    /**
     * Gets an item from the items-v3.yml file and automatically creates it.
     *
     * @param   locale
     *          The locale
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

    /**
     * Gets an item from the items-v3.yml file and automatically creates it.
     *
     * @param   user
     *          The user
     *
     * @param   path
     *          The path of the item (excluding the parameters and 'items.')
     *
     * @param   replace
     *          What should be replaced in the lore/name
     *
     * @return the item based on the data from items-v3.yml
     */
    public Item getFromItemData(@Nullable ParkourUser user, String path, @Nullable String... replace) {
        return getFromItemData(user == null ? Option.DEFAULT_LOCALE : user.getLocale(), path, replace);
    }

    private ItemData getItemData(String path, String locale, @Nullable String... replace) {
        FileConfiguration config = getFile("items");

        String namePath = "locale." + locale + "." + path;
        String matPath = "items." + path;

        String name = config.getString(namePath + ".name");

        if (name != null && replace != null && replace.length > 0) {
            name = name.replaceFirst("%[a-z]", replace[0] == null ? "" : replace[0]);
        }

        String l = config.getString(namePath + ".lore");

        List<String> lore = null;
        if (l != null) {
            lore = Arrays.asList(l.split("\\|\\|"));
            if (lore.size() != 0 && replace != null && replace.length > 0) {
                List<String> copy = new ArrayList<>();
                int index = 0;
                for (String s : lore) {
                    copy.add(s.replaceFirst("%[a-z]", replace[index] == null ? "" : replace[index]));
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
    private record ItemData(String name, List<String> lore, @Nullable Material material) {

    }
}