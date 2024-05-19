package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Locale message/item handler.
 */
public class Locales {

    // a list of all nodes, used to check against missing nodes
    private static List<String> resourceNodes;

    /**
     * A map of all locales with their respective yml trees
     */
    public static final Map<String, FileConfiguration> locales = new HashMap<>();

    /**
     * Initializes this Locale handler.
     */
    public static void init() {
        Plugin plugin = IP.getPlugin();

        Task.create(plugin).async().execute(() -> {
            locales.clear();

            FileConfiguration embedded = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("locales/en.yml"), StandardCharsets.UTF_8));

            // get all nodes from the plugin's english resource, aka the most updated version
            resourceNodes = getChildren(embedded, "", true);

            File folder = IP.getInFolder("locales");

            // download files to locales folder
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String[] files = folder.list();

            // create non-existent files
            if (files != null && files.length == 0) {
                plugin.saveResource("locales/en.yml", false);
                plugin.saveResource("locales/nl.yml", false);
                plugin.saveResource("locales/fr.yml", false);
                plugin.saveResource("locales/zh_cn.yml", false);
            }

            // get all files in locales folder
            try (Stream<Path> stream = Files.list(folder.toPath())) {
                stream.forEach(path -> {
                    File file = path.toFile();

                    // get locale from file name
                    String locale = file.getName().split("\\.")[0];

                    IP.log("Found locale " + locale);

                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    validate(embedded, config, file);

                    locales.put(locale, config);
                });
            } catch (Exception ex) {
                IP.logging().stack("Error while trying to read locale files", "restart/reload your server", ex);
            }
        }).run();
    }

    // validates whether a lang file contains all required keys.
    // if it doesn't, automatically add them
    private static void validate(FileConfiguration provided, FileConfiguration user, File localPath) {
        List<String> userNodes = getChildren(user, "", true);

        for (String node : resourceNodes) {
            if (userNodes.contains(node)) {
                continue;
            }
          
            IP.log("Fixing missing config node %s in %s".formatted(node, localPath.getName()));

            user.set(node, provided.get(node));
        }

        IP.log("Validated locale " + localPath.getName());

        try {
            user.save(localPath);
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save fixed config file %s".formatted(localPath), "delete this file and restart your server", ex);
        }
    }

    /**
     * Gets a String from the provided path in the provided player's locale.
     * If the player is a {@link ParkourPlayer2}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param player The player
     * @param path   The path
     * @return a String
     */
    public static String getString(Player player, String path) {
        var user = ParkourPlayer2.as(player);
        var locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        return getString(locale, path);
    }

    public static String getString(ParkourPlayer2 player, String path) {
        String locale = player == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : player.locale;

        return getString(locale, path);
    }

    /**
     * Gets a coloured String from the provided path in the provided locale file
     *
     * @param locale The locale
     * @param path   The path
     * @return a String
     */
    public static String getString(String locale, String path) {
        return getValue(locale, config -> config.getString(path), "");
    }

    /**
     * Gets an uncoloured String list from the provided path in the provided locale file
     *
     * @param locale The locale
     * @param path   The path
     * @return a String list
     */
    public static List<String> getStringList(String locale, String path) {
        return getValue(locale, config -> config.getStringList(path), Collections.emptyList());
    }

    private static <T> T getValue(String locale, Function<FileConfiguration, T> f, T def) {
        if (locales.isEmpty()) {
            return def;
        }

        FileConfiguration config = locales.get(locale);

        return config != null ? f.apply(config) : def;
    }

    /**
     * Returns an item from a json locale file.
     * The locale is derived from the player.
     * If the player is a {@link ParkourUser}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param player The player
     * @param path   The full path of the item in the locale file
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    @NotNull
    public static Item getItem(@NotNull Player player, String path, String... replace) {
        var user = ParkourPlayer2.as(player);
        var locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        return getItem(locale, path, replace);
    }

    private static final Pattern pattern = Pattern.compile("%[a-z]");

    /**
     * Returns an item from a provided json locale file with possible replacements.
     *
     * @param locale  The locale
     * @param path    The path in the json file
     * @param replace The Strings that will replace any appearances of a String following the regex "%[a-z]"
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    @NotNull
    public static Item getItem(String locale, String path, String... replace) {
        if (locales.isEmpty()) { // during reloading
            return new Item(Material.STONE, "");
        }

        FileConfiguration base = locales.get(locale);

        String material = base.getString("%s.material".formatted(path));
        String name = base.getString("%s.name".formatted(path));
        String lore = base.getString("%s.lore".formatted(path));

        if (material == null) {
            material = "";
        }
        if (name == null) {
            name = "";
        }
        if (lore == null) {
            lore = "";
        }

        int idx = 0;
        Matcher matcher = pattern.matcher(name);
        while (matcher.find()) {
            if (idx == replace.length) {
                break;
            }

            name = name.replaceFirst(matcher.group(), replace[idx]);
            idx++;
        }

        matcher = pattern.matcher(lore);

        while (matcher.find()) {
            if (idx == replace.length) {
                break;
            }

            lore = lore.replaceFirst(matcher.group(), replace[idx]);
            idx++;
        }

        Item item = new Item(Material.getMaterial(material.toUpperCase()), name);

        if (!lore.isEmpty()) {
            item.lore(lore.split("\\|\\|"));
        }

        return item;
    }

    public static List<String> getChildren(FileConfiguration file, String path, boolean deep) {
        ConfigurationSection section = file.getConfigurationSection(path);

        return section != null ? new ArrayList<>(section.getKeys(deep)) : Collections.emptyList();
    }
}