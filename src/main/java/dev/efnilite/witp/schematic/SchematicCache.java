package dev.efnilite.witp.schematic;

import dev.efnilite.witp.WITP;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicCache {

    public static Map<String, Schematic> cache = new HashMap<>();

    public static void read() {
        File folder = new File(WITP.getInstance().getDataFolder() + "/structures/");
        List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.contains("parkour-")));
        for (File file : files) {
            String fileName = file.getName();
            Schematic schematic = new Schematic().file(fileName);
            schematic.read();
            cache.put(fileName, schematic);
        }
    }

    public static Schematic getSchematic(String name) {
        return cache.get(name);
    }
}