package dev.efnilite.ip.config;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.vilib.lib.configupdater.configupdater.ConfigUpdater;
import dev.efnilite.vilib.util.Task;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Config management class.
 */
public enum Config {

    CONFIG("config.yml"),
    GENERATION("generation.yml"),
    REWARDS("rewards-v2.yml"),
    SCHEMATICS("schematics.yml");

    /**
     * Reloads all config files.
     */
    public static void reload() {
        for (Config config : values()) {
            config.load();
        }

        // read rewards file
        Rewards.init();
        Locales.init();

        if (schematics()) {
            // read schematics again
            Schematics.init();
        }

        IP.logging().info("Loaded all config files");
    }

    // todo fix
    private static boolean schematics() {
        if (IP.getInFolder("schematics/spawn-island-duels.witp").exists()) {
            return true;
        }

        String[] schematics = new String[]{"spawn-island.witp", "spawn-island-duels.witp"};
        File folder = IP.getInFolder("schematics");
        folder.mkdirs();

        IP.logging().info("Downloading missing schematics...");
        int structureCount = 21;

        Task.create(IP.getPlugin()).async().execute(() -> {
            try {
                for (String schematic : schematics) {
                    Path path = Paths.get(folder.toString(), schematic);
                    if (path.toFile().exists()) {
                        continue;
                    }

                    InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/" + schematic).openStream();
                    Files.copy(stream, path);
                    stream.close();
                }
                for (int i = 1; i <= structureCount; i++) {
                    Path path = Paths.get(folder.toString(), "parkour-%d.witp".formatted(i));
                    if (path.toFile().exists()) {
                        continue;
                    }

                    InputStream stream = new URL("https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/parkour-" + i + ".witp").openStream();
                    Files.copy(stream, path);
                    stream.close();
                }

                Schematics.init();
                IP.logging().info("Downloaded all schematics");
            } catch (FileAlreadyExistsException ex) {
                // do nothing
            } catch (UnknownHostException ex) {
                IP.logging().stack("Stopped download of schematics", "join the Discord and send this error to receive help", ex);
            } catch (Throwable throwable) {
                IP.logging().stack("Stopped download of schematics", "delete the schematics folder and restart the server", throwable);
            }
        }).run();

        return false;
    }

    /**
     * The {@link FileConfiguration} instance associated with this config file.
     */
    public FileConfiguration fileConfiguration;

    /**
     * The path to this file, incl. plugin folder.
     */
    public final File path;

    /**
     * The name of this file, e.g. config.yml
     */
    public final String fileName;

    Config(String fileName) {
        this.fileName = fileName;
        this.path = IP.getInFolder(fileName);

        if (!path.exists()) {
            IP.getPlugin().saveResource(fileName, false);
            IP.logging().info("Created config file %s".formatted(fileName));
        }

        update();
        load();
    }

    /**
     * Loads the file from disk.
     */
    public void load() {
        this.fileConfiguration = YamlConfiguration.loadConfiguration(path);
    }

    /**
     * Updates the file so all keys are present.
     */
    public void update() {
        try {
            ConfigUpdater.update(IP.getPlugin(), fileName, path, switch (fileName) {
                case "config.yml" -> List.of("styles");
                case "schematics.yml" -> List.of("difficulty");
                default -> null;
            });
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to update config file", ex);
        }
    }

    /**
     * @param path The path.
     * @return True when path exists, false if not.
     */
    public boolean isPath(@NotNull String path) {
        return fileConfiguration.isSet(path);
    }

    /**
     * @param path The path.
     * @return The value at path.
     */
    public Object get(@NotNull String path) {
        check(path);

        return fileConfiguration.get(path);
    }


    /**
     * @param path The path.
     * @return The boolean value at path.
     */
    public boolean getBoolean(@NotNull String path) {
        check(path);

        return fileConfiguration.getBoolean(path);
    }

    /**
     * @param path The path.
     * @return The int value at path.
     */
    public int getInt(@NotNull String path) {
        check(path);

        return fileConfiguration.getInt(path);
    }

    /**
     * @param path The path.
     * @return The double value at path.
     */
    public double getDouble(@NotNull String path) {
        check(path);

        return fileConfiguration.getDouble(path);
    }

    /**
     * @param path The path.
     * @return The String value at path.
     */
    @NotNull
    public String getString(@NotNull String path) {
        check(path);

        return fileConfiguration.getString(path, "");
    }

    /**
     * @param path The path.
     * @return The String list value at path.
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        check(path);

        return fileConfiguration.getStringList(path);
    }

    /**
     * @param path The path.
     * @return The int list value at path.
     */
    @NotNull
    public List<Integer> getIntList(@NotNull String path) {
        check(path);

        return fileConfiguration.getIntegerList(path);
    }

    /**
     * @param path The path.
     * @param deep Whether search should include children of children as well.
     * @return The children nodes from path.
     */
    @NotNull
    public List<String> getChildren(@NotNull String path, boolean... deep) {
        check(path);

        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);

        if (section == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(section.getKeys(deep != null));
    }

    // checks if the specified path exists to avoid developer error
    private void check(@NotNull String path) {
        if (!isPath(path)) {
            throw new IllegalStateException("Unknown path %s in %s".formatted(path, fileName));
        }
    }
}
