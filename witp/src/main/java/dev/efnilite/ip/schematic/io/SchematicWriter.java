package dev.efnilite.ip.schematic.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schematic writing handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicWriter {

    /**
     * Saves blocks to the specified file.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     */
    public void save(File file, Location pos1, Location pos2) {
        List<Block> blocks = getBlocks(Locations.min(pos1, pos2), Locations.max(pos1, pos2));

        Map<String, Integer> palette = getPalette(blocks);
        Map<String, Integer> offsetData = getOffsetData(blocks, palette);

        // write to file
        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(Schematic.VERSION);
            stream.writeObject(palette);
            stream.writeObject(offsetData);
            stream.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save schematic %s".formatted(file), ex);
        }
    }


    // returns all blocks between the min location (minL) and max location (maxL)
    private List<Block> getBlocks(Location minL, Location maxL) {
        List<Block> blocks = new ArrayList<>();
        Location location = new Location(minL.getWorld() == null ? maxL.getWorld() : minL.getWorld(), 0, 0, 0);

        for (int x = minL.getBlockX(); x <= maxL.getBlockX(); x++) {
            for (int y = minL.getBlockY(); y <= maxL.getBlockY(); y++) {
                for (int z = minL.getBlockZ(); z <= maxL.getBlockZ(); z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    if (location.getBlock().getType() == Material.AIR) {
                        continue;
                    }

                    blocks.add(location.getBlock());
                }
            }
        }
        return blocks;
    }


    // returns block palette where ids are used instead of the full blockdata to reduce file space
    private Map<String, Integer> getPalette(List<Block> blocks) {
        Map<String, Integer> palette = new HashMap<>();

        blocks.stream()
            .map(block -> block.getBlockData().getAsString())
            .distinct()
            .forEach(datum -> palette.put(datum, palette.size()));

        return palette;
    }

    // returns the offset data, where each block offset (position relative to minimum location)
    // is mapped to the data at that position.
    // the first item in the array is the generic BlockData of that block.
    // the second item in the array is the special data of that block, as encoded by the State instances.
    private Map<String, Integer> getOffsetData(List<Block> blocks, Map<String, Integer> palette) {
        List<Location> locations = blocks.stream().map(Block::getLocation).toList();
        Location min = locations.stream().reduce(Locations::min).orElseThrow();

        Map<String, Integer> offsetData = new HashMap<>();

        for (Location loc : locations) {
            offsetData.put(loc.subtract(min).toVector().toString(), palette.get(loc.getBlock().getBlockData().getAsString()));
        }

        return offsetData;
    }
}
