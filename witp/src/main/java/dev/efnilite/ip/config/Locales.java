package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Locales {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    // a map of all locales with their respective json trees
    // the json trees are stored instead of the files to avoid having to read the files every time
    private static final Map<String, FileConfiguration> localeTree = new HashMap<>();

    public static void init(Plugin plugin) {
        Task.create(plugin)
                .async()
                .execute(() -> {
                    Path folder = Paths.get(plugin.getDataFolder() + "/locales");

                    // download files to locales folder
                    if (!folder.toFile().exists()) {
                        folder.toFile().mkdirs();

                        plugin.saveResource("locales/en.yml", false);
                    }

                    // get all files in locales folder
                    try (Stream<Path> stream = Files.list(folder)) {
                        stream.forEach(path -> {
                            File file = path.toFile();

                            // get locale from file name
                            String locale = file.getName().split("\\.")[0];

                            localeTree.put(locale, YamlConfiguration.loadConfiguration(file));
                        });
                    } catch (IOException throwable) {
                        IP.logging().stack("Error while trying to read locale files", "restart/reload your server", throwable);
                    }
                })
                .run();
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
    public static String getString(Player player, String path) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LOCALE : user.getLocale();

        return getString(locale, path);
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
    public static String getString(String locale, String path) {
        FileConfiguration base = localeTree.get(locale);

        if (base == null) {
            return "";
        }

        String string = base.getString(path);

        if (string == null) {
            return "";
        }

        return colour(string);
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
    public static Item getItem(Player player, String path, String... replace) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LOCALE : user.getLocale();

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
        final FileConfiguration base = localeTree.get(locale);

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

    /**
     * Colours a provided string using the MiniMessage API.
     *
     * @param   string
     *          The string
     *
     * @return the coloured string
     */
    public static String colour(String string) {
        Component component = miniMessage.deserialize(string);

        return miniMessage.serialize(component);
    }

}
