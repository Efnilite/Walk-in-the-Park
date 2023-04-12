package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.legacy.LegacySchematicMigrator;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    private static final int SCHEMATIC_COUNT = 21;

    private static final String URL = "https://github.com/Efnilite/Walk-in-the-Park/raw/main/schematics/";

    private static final File FOLDER = IP.getInFolder("schematics");

    public static final Map<String, Schematic> CACHE = new HashMap<>();

    /**
     * Reads all files.
     */
    public static void init() {
        Task.create(IP.getPlugin()).async().execute(() -> {
            Time.timerStart("ip load schematics");

            if (!FOLDER.exists()) {
                FOLDER.mkdirs();
            }

            new LegacySchematicMigrator().migrate();

            File[] files = FOLDER.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));

            if (files == null || files.length == 0) {
                download();
                return;
            }

            CACHE.clear();
            for (File file : files) {
                Schematic schematic = Schematic.create().load(file);

                if (schematic.isSupported()) {
                    CACHE.put(file.getName(), schematic);
                }
            }

            IP.logging().info("Found %d unsupported schematic(s).".formatted(files.length - CACHE.keySet().size()));
            IP.logging().info("Loaded all schematics in %d ms!".formatted(Time.timerEnd("ip load schematics")));
        }).run();
    }

    // downloads all schematics
    public static void download() {
        IP.logging().info("Downloading schematics...");

        List<String> schematics = new ArrayList<>();
        schematics.addAll(Arrays.asList("spawn-island", "spawn-island-duels"));
        schematics.addAll(Colls.range(1, SCHEMATIC_COUNT + 1).stream().map("parkour-%d"::formatted).toList());
        schematics.forEach(Schematics::downloadFile);
    }

    // downloads a singular file to the schematics folder
    private static void downloadFile(String name) {
        try (InputStream stream = new URL("%s%s".formatted(URL, name)).openStream()) {
            Files.copy(stream, Paths.get(FOLDER.toString(), name));
        } catch (FileAlreadyExistsException ex) {
            // do nothing
        } catch (UnknownHostException ex) {
            IP.logging().stack("Internet error while downloading schematic %s".formatted(name), ex);
        } catch (Exception ex) {
            IP.logging().stack("Error while downloading schematic %s".formatted(name), ex);
        }
    }
}