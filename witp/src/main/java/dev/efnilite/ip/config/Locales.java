package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Strings;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Material;
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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Locales {

    // a list of all nodes
    // used to check against missing nodes
    private static List<String> resourceNodes;

    // a map of all locales with their respective yml trees
    private static final Map<String, FileConfiguration> locales = new HashMap<>();

    public static void init(Plugin plugin) {
        Task.create(plugin)
                .async()
                .execute(() -> {
                    FileConfiguration resource = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("locales/en.yml"), StandardCharsets.UTF_8));

                    // get all nodes from the plugin's english resource, aka the most updated version
                    resourceNodes = Util.getNode(resource, "", true);

                    Path folder = Paths.get(plugin.getDataFolder() + "/locales");

                    // download files to locales folder
                    if (!folder.toFile().exists()) {
                        folder.toFile().mkdirs();

                        plugin.saveResource("locales/en.yml", false);
                        plugin.saveResource("locales/nl.yml", false);
                    }

                    // get all files in locales folder
                    try (Stream<Path> stream = Files.list(folder)) {
                        stream.forEach(path -> {
                            File file = path.toFile();

                            // get locale from file name
                            String locale = file.getName().split("\\.")[0];

                            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                            validate(resource, config, file);

                            locales.put(locale, config);
                        });
                    } catch (IOException throwable) {
                        IP.logging().stack("Error while trying to read locale files", "restart/reload your server", throwable);
                    }
                })
                .run();
    }

    // validates whether a lang file contains all required keys.
    // if it doesn't, automatically add them
    private static void validate(FileConfiguration provided, FileConfiguration user, File localPath) {
        List<String> userNodes = Util.getNode(user, "", true);

        for (String node : resourceNodes) {
            if (!userNodes.contains(node)) {
                IP.logging().info("Fixing missing config node '" + node + "'");

                Object providedValue = provided.get(node);
                user.set(node, providedValue);
            }
        }

        try {
            user.save(localPath);
        } catch (IOException throwable) {
            IP.logging().stack("Error while trying to save fixed config file " + localPath, "delete this file and restart your server", throwable);
        }
    }

    /**
     * Gets a coloured String from the provided path in the provided locale file.
     * The locale is derived from the player.
     * If the player is a {@link ParkourUser}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param   player
     *          The player
     *
     * @param   path
     *          The path
     *
     * @return a coloured String
     */
    public static String getString(Player player, String path, boolean colour) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.getLocale();

        return getString(locale, path, colour);
    }

    /**
     * Gets a coloured String from the provided path in the provided locale file
     *
     * @param   locale
     *          The locale
     *
     * @param   path
     *          The path
     *
     * @return a coloured String
     */
    public static String getString(String locale, String path, boolean colour) {
        FileConfiguration base = locales.get(locale);

        if (base == null) {
            return "";
        }

        String string = base.getString(path);

        if (string == null) {
            return "";
        }

        return colour ? Strings.colour(string) : string;
    }

    /**
     * Gets a coloured String list from the provided path in the provided locale file
     *
     * @param   locale
     *          The locale
     *
     * @param   path
     *          The path
     *
     * @return a coloured String list
     */
    public static List<String> getStringList(String locale, String path, boolean colour) {
        FileConfiguration base = locales.get(locale);

        if (base == null) {
            return Collections.emptyList();
        }

        List<String> strings = base.getStringList(path);

        if (strings.isEmpty()) {
            return Collections.emptyList();
        }

        return colour ? strings.stream().map(Strings::colour).collect(Collectors.toList()) : strings;
    }

    /**
     * Returns an item from a json locale file.
     * The locale is derived from the player.
     * If the player is a {@link ParkourUser}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param   player
     *          The player
     *
     * @param   path
     *          The full path of the item in the locale file
     *
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    @NotNull
    public static Item getItem(@NotNull Player player, String path, String... replace) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.getLocale();

        return getItem(locale, path, replace);
    }

    /**
     * Returns an item from a provided json locale file with possible replacements.
     *
     * @param   locale
     *          The locale
     *
     * @param   path
     *          The path in the json file
     *
     * @param   replace
     *          The Strings that will replace any appearances of a String following the regex "%[a-z]"
     *
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    @NotNull
    public static Item getItem(String locale, String path, String... replace) {
        final FileConfiguration base = locales.get(locale);

        String material = base.getString(path + ".material");
        String name = base.getString(path + ".name");
        String lore = base.getString(path + ".lore");

        if (material == null) {
            material = "";
        }
        if (name == null) {
            name = "";
        }
        if (lore == null) {
            lore = "";
        }

        Pattern pattern = Pattern.compile("%[a-z]");
        Matcher matcher = pattern.matcher(name);

        int index = 0;
        while (matcher.find()) {
            if (index == replace.length) {
                break;
            }

            name = name.replaceFirst(matcher.group(), replace[index]);
            index++;
        }

        matcher = pattern.matcher(lore);

        while (matcher.find()) {
            if (index == replace.length) {
                break;
            }

            lore = lore.replaceFirst(matcher.group(), replace[index]);
            index++;
        }

        Item item = new Item(Material.getMaterial(material.toUpperCase()), name);

        if (!lore.isEmpty()) {
            item.lore(lore.split("\\|\\|"));
        }

        return item;
    }

    public static Map<String, FileConfiguration> getLocales() {
        return locales;
    }
}