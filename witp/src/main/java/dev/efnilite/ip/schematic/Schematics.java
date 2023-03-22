package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    public static volatile Map<String, Schematic> cache = new HashMap<>();

    /**
     * Reads all files.
     */
    public static void init() {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    Time.timerStart("ip load schematics");

                    cache.clear();
                    File folder = IP.getInFolder("schematics");

                    File[] files = folder.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));

                    if (files == null) {
                        return;
                    }

                    for (File file : files) {
                        String fileName = file.getName();

                        Schematic schematic = new Schematic().file(fileName);
                        schematic.read();

                        if (schematic.isSupported()) {
                            cache.put(fileName, schematic);
                        }
                    }

                    IP.logging().info("Found %d unsupported schematic(s).".formatted(files.length - cache.keySet().size()));
                    IP.logging().info("Loaded all schematics in %d ms!".formatted(Time.timerEnd("ip load schematics")));
                })
                .run();
    }

    /**
     * @param name The name.
     * @return A schematic instance by name.
     */
    public static Schematic getSchematic(String name) {
        return cache.get(name);
    }
}