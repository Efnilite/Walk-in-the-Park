package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.legacy.LegacySchematicMigrator;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;

import java.io.File;
import java.util.*;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    private static final String[] SPAWN_SCHEMATICS = new String[] {
            "spawn-island", "spawn-island-duels"
    };
    private static final String[] PARKOUR_SCHEMATICS = new String[] {
            "686ebc21", "428fcf54", "323ab385", "b79cc16b", "31df2b96",
            "f9d2c0ac", "d166d78f", "0507171d", "93570681", "bd89f053",
            "98d1f8e8", "e923ac20", "74b4fd1e", "befcc649", "1d1a7f00",
            "e037702e", "e772a8f0", "2bd47c9f", "432623a9", "34037304",
            "bd7b4c19", "6fdae317", "51f1e203", "6797dbf9", "135f8b2b",
            "365409ca", "72ec530f", "8a7575ec", "fa035723", "1d5969be",
            "09ba8fa3", "1dc87b41", "bb99d8bc", "ea9f7e05", "8dfd98c1"
    };

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

    private static void download() {
        List<String> schematics = new ArrayList<>();
        schematics.addAll(Arrays.asList(SPAWN_SCHEMATICS));
        schematics.addAll(Arrays.stream(PARKOUR_SCHEMATICS).map("parkour-%s"::formatted).toList());
        schematics.forEach(file -> IP.getPlugin().saveResource("schematics/%s".formatted(file), true));
    }
}