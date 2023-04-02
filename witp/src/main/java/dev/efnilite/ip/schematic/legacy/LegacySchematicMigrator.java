package dev.efnilite.ip.schematic.legacy;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
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
        try (Stream<Path> stream = Files.walk(IP.getInFolder("schematics/").toPath())
                .filter(path -> path.getFileName().endsWith(".witp"))) {

            stream.map(Path::toFile)
                    .forEach(file -> {
                        read(file);
                        file.delete();
                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void read(File file) {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            lines = reader.lines().toList(); // read the lines of the file
        } catch (IOException ex) {
            IP.logging().stack("Error while reading file", ex);
            return;
        }

        Map<String, Integer> palette = getPalette(lines);
        Map<String, Object[]> offsetData = getOffsetData(lines, palette);

        // todo change file name

        // write to file
        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(palette);
            stream.writeObject(offsetData);
            stream.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save schematic %s".formatted(file), ex);
        }
    }

    private Map<String, Integer> getPalette(List<String> lines) {
        Map<String, Integer> palette = new HashMap<>();

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
                palette.put(Bukkit.createBlockData(elements[1]).getAsString(), Integer.parseInt(elements[0]));
            }
        }

        return palette;
    }


    private final Pattern idPattern = Pattern.compile("^\\d+");
    private final Pattern vectorPattern = Pattern.compile("\\(-?\\d+,-?\\d+,-?\\d+\\)");

    private Map<String, Object[]> getOffsetData(List<String> lines, Map<String, Integer> palette) {
        Map<String, Object[]> offsetData = new HashMap<>();

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
            Vector vector = null;
            while (vectorMatcher.find()) {
                vector = parseVector(vectorMatcher.group());
            }

            offsetData.put(vector.toString(), new Object[] { getFromId(palette, id), null});
        }

        return offsetData;
    }

    private String getFromId(Map<String, Integer> palette, int id) {
        return Colls.thread(palette)
                .filter((k, v) -> v == id)
                .get()
                .keySet()
                .stream()
                .findAny()
                .orElseThrow();
    }

    private Vector parseVector(String vector) {
        String[] split = vector.replaceAll("[()]", "").split(",");
        return new Vector(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }
}