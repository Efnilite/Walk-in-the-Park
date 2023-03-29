package dev.efnilite.ip.schematic.v2.io;

import dev.efnilite.ip.schematic.v2.Schematic2;
import org.bukkit.block.Block;

import java.io.File;
import java.util.List;

/**
 * Handles schematic saving/reading.
 */
// todo entity support? save params to location extremes
public interface SchematicIO {

    /**
     * Saves blocks to the specified file.
     * @param file The file.
     * @param blocks The list of blocks to save.
     */
    void save(File file, List<Block> blocks);

    /**
     * @param file The file.
     * @return A new {@link Schematic2} instance based on the read blocks.
     */
    Schematic2 read(File file);

}
