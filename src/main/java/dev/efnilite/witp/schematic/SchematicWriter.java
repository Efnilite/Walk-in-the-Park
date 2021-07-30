package dev.efnilite.witp.schematic;

import dev.efnilite.witp.schematic.selection.Dimensions;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * A class for writing schematics (i/o)
 *
 * Taken from: Efnilite/Redaktor
 */
public class SchematicWriter {

    /**
     * The file
     */
    private final String file;

    /**
     * Creates a new instance
     *
     * @param   file
     *          The file
     */
    public SchematicWriter(String file) {
        this.file = file.endsWith(".witp") ? file : file + ".witp";
    }

    /**
     * Writes a {@link Schematic} to a file
     *
     * @param   blocks
     *          The blocks of the schematic
     *
     * @throws  IOException
     *          If something goes wrong
     */
    public void write(List<Block> blocks, Dimensions dimensions) throws IOException {
        FileWriter writer = new FileWriter(file);
        HashSet<String> filter = new HashSet<>();
        HashMap<String, Integer> palette = new HashMap<>();
        String separator = System.lineSeparator();

        writer.write(dimensions.toString() + separator);

        writer.write("*");
        writer.write(separator);

        blocks.forEach(block -> filter.add(block.getBlockData().getAsString()));

        int index = 0;
        for (String data : filter) {
            palette.put(data, index);
            writer.write(index + " > " + data + separator);
            index++;
        }

        writer.write("~" + separator);

        List<String> fullBlocks = new ArrayList<>();
        for (Block block : new LinkedList<>(blocks)) {
            String current = block.getBlockData().getAsString();
            String id = Integer.toString(palette.get(current));

            fullBlocks.add(id);
        }

        writer.write(compress(fullBlocks));
        writer.close();
    }

    /**
     * Compresses the string
     *
     * Source: https://codereview.stackexchange.com/a/65340
     *
     * @param   string
     *          The string
     *
     * @return a compressed string
     */
    private String compress(List<String> string) {
        if (string.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        int count = 1;
        String previous = string.get(0);
        for (int i = 1; i < string.size(); i++) {
            String current = string.get(i);
            if (current.equals(previous)) {
                count++;
            } else {
                builder.append(previous).append("x").append(count).append(",");
                count = 1;
            }
            previous = current;
        }
        return builder.append(previous).append("x").append(count).toString();
    }

    /**
     * Gets the file
     *
     * @return the file
     */
    public String getFile() {
        return file;
    }
}
