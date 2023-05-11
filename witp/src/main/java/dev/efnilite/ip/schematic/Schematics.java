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
            "d0e227d2", "dbef835e", "27f41406", "a359eb36", "5b7fdc6c",
            "4a67b679", "3f66b71e", "b951e6ec", "7ba9f27e", "fce2a825",
            "194fdb81", "9078de5a", "def6f9d9", "3fda7be9", "9debcf7f",
            "c086c587", "c27bbddb", "564cf053", "47770924", "0442a82a",
            "cf9b8e48", "11a03fac", "3513c613", "3087330f", "7006faf0",
            "a05f3bc0", "d7bc4762", "dee7511a", "2b36717a", "b1b4d7bc",
            "7de341cf", "9d08e9fc", "7ecc32fe", "21e87090", "c59e233e"
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
                init();
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