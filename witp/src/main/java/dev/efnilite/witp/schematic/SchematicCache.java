package dev.efnilite.witp.schematic;

import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.witp.IP;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores schematics, so they don't have to be read every time
 */
public class SchematicCache {

    public static volatile Map<String, Schematic> cache = new HashMap<>();

    public static void read() {
        if (!IP.versionSupportsSchematics()) {
            Logging.warn("This version does *not* support schematics, consider upgrading if you want them");
            return;
        }

        new Task()
                .async()
                .execute(() -> {
                    Time.timerStart("schematicsLoad");
                    Logging.info("Initializing schematics...");
                    cache.clear();
                    File folder = new File(IP.getInstance().getDataFolder() + "/schematics/");
                    File[] files = folder.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));
                    for (File file : files) {
                        String fileName = file.getName();
                        Schematic schematic = new Schematic().file(fileName);
                        schematic.read();
                        if (schematic.isSupported()) {
                            cache.put(fileName, schematic);
                        }
                    }
                    Logging.info("Found " + (files.length - cache.keySet().size()) + " unsupported schematic(s).");
                    Logging.info("Loaded all schematics in " + Time.timerEnd("schematicsLoad") + "ms!");
                })
                .run();
    }

    public static Schematic getSchematic(String name) {
        return cache.get(name);
    }
}