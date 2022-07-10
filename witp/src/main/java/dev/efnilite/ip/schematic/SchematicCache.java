package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;

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
            IP.logging().warn("This version does *not* support schematics, consider upgrading if you want them");
            return;
        }

        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    Time.timerStart("schematicsLoad");
                    IP.logging().info("Initializing schematics...");
                    cache.clear();
                    File folder = new File(IP.getPlugin().getDataFolder() + "/schematics/");
                    File[] files = folder.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));
                    for (File file : files) {
                        String fileName = file.getName();
                        Schematic schematic = new Schematic().file(fileName);
                        schematic.read();
                        if (schematic.isSupported()) {
                            cache.put(fileName, schematic);
                        }
                    }
                    IP.logging().info("Found " + (files.length - cache.keySet().size()) + " unsupported schematic(s).");
                    IP.logging().info("Loaded all schematics in " + Time.timerEnd("schematicsLoad") + "ms!");
                })
                .run();
    }

    public static Schematic getSchematic(String name) {
        return cache.get(name);
    }

    public static void invalidate() {
        cache.clear();
    }
}