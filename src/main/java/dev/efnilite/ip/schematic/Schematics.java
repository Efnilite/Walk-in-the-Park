package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.schematic.legacy.LegacySchematicMigrator;
import dev.efnilite.vilib.schematic.Schematic;
import dev.efnilite.vilib.util.Task;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    public static final Map<String, Schematic> CACHE = new HashMap<>();
    private static final String[] SPAWN_SCHEMATICS = new String[]{
            "spawn-island", "spawn-island-duels"
    };
    private static final File FOLDER = IP.getInFolder("schematics");

    /**
     * Reads all files.
     */
    public static void init() {
        Task.create(IP.getPlugin()).async().execute(() -> {
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
                try {
                    Schematic schematic = Schematic.create().load(file);

                    if (!schematic.isSupported()) {
                        IP.logging().warn("Schematic %s is not supported.".formatted(file.getName()));
                        continue;
                    }

                    CACHE.put(file.getName(), schematic);
                } catch (ExecutionException | InterruptedException ex) {
                    IP.logging().stack("Error while trying to load schematic", ex);
                }
            }
        }).run();
    }

    private static void download() {
        List<String> schematics = new ArrayList<>();
        schematics.addAll(Arrays.asList(SPAWN_SCHEMATICS));
        schematics.addAll(Config.SCHEMATICS.getChildren("difficulty", false).stream().map("parkour-%s"::formatted).toList());
        schematics.forEach(file -> IP.getPlugin().saveResource("schematics/%s".formatted(file), true));
    }
}