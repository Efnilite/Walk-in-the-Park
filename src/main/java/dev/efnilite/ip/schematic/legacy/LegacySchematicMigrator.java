package dev.efnilite.ip.schematic.legacy;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Colls;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LegacySchematicMigrator {

    public void migrate() {
        try (Stream<Path> stream = Files.walk(IP.getInFolder("schematics").toPath())
                .filter(path -> path.toFile().getName().endsWith(".witp"))) {

            stream.map(Path::toFile)
                .forEach(file -> {
                    IP.log("Migrating schematic %s".formatted(file.getName()));

                    migrate(file);
                    file.delete();
                });

        } catch (IOException ex) {
            IP.logging().stack("Error while migrating schematics", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void migrate(File file) {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            lines = reader.lines().toList(); // read the lines of the file
        } catch (IOException ex) {
            IP.logging().stack("Error while reading file", ex);
            return;
        }

        Object[] data = getData(lines, getPossibleDuplicatePalette(lines));

        Map<String, Integer> palette = (Map<String, Integer>) data[0];
        Map<String, Integer> offsetData = (Map<String, Integer>) data[1];

        file = new File(file.getParent(), file.getName().split("\\.")[0]);

        // write to file
        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(1); // version
            stream.writeObject(palette);
            stream.writeObject(offsetData);
            stream.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save schematic %s".formatted(file), ex);
        }
    }

    private Map<Integer, String> getPossibleDuplicatePalette(List<String> lines) {
        Map<Integer, String> palette = new HashMap<>();

        boolean readingPalette = false; // palette is ? lines long
        for (String string : lines) { // reads the palette
            if (string.contains("*")) {
                readingPalette = true;
                continue;
            } else if (string.contains("~")) {
                break;
            }
            if (readingPalette) {
                String[] elements = string.split(">");
                palette.put(Integer.parseInt(elements[0]), Bukkit.createBlockData(elements[1]).getAsString());
            }
        }

        return palette;
    }

    private final Pattern idPattern = Pattern.compile("^\\d+");
    private final Pattern vectorPattern = Pattern.compile("\\(-?\\d+,-?\\d+,-?\\d+\\)");

    private Object[] getData(List<String> lines, Map<Integer, String> palette) {
        Map<String, String> intermediateOffsetData = new HashMap<>();

        String fileBlocks = lines.get(lines.size() - 1);
        String[] splitBlocks = fileBlocks.split("/");

        // -- Writes it to the file and adds it to the blocks --

        for (String block : splitBlocks) { // parse the SchematicBlocks

            Matcher idMatcher = idPattern.matcher(block); // finds the id
            int id = 0;
            while (idMatcher.find()) {
                id = Integer.parseInt(idMatcher.group());
            }

            Matcher vectorMatcher = vectorPattern.matcher(block);
            Vector vector = new Vector();
            while (vectorMatcher.find()) {
                vector = parseVector(vectorMatcher.group());
            }

            intermediateOffsetData.put(vector.toString(), palette.get(id));
        }

        // removes duplicates that apparently are present in schematics v1
        Map<String, Integer> noDuplicatePalette = new HashMap<>();
        intermediateOffsetData.values().stream().distinct().forEach(data -> noDuplicatePalette.put(data, noDuplicatePalette.size()));

        Map<String, Integer> offsetData = Colls.thread(intermediateOffsetData)
                .mapv((offset, data) -> noDuplicatePalette.get(data))
                .get();

        return new Object[] { noDuplicatePalette, offsetData };
    }

    private Vector parseVector(String vector) {
        String[] split = vector.replaceAll("[()]", "").split(",");
        return new Vector(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }
}