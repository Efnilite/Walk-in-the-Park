package dev.efnilite.ip.schematic.v2.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.v2.Schematic2;
import dev.efnilite.ip.schematic.v2.data.StateCreatureSpawner;
import dev.efnilite.ip.schematic.v2.data.StateSign;
import dev.efnilite.ip.schematic.v2.data.StateSkull;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicIO {

    /**
     * Saves blocks to the specified file.
     * @param file The file.
     * @param blocks The list of blocks to save.
     */
    public void save(File file, List<Block> blocks) {
        // palette: maps blockdata to ints to reduce file size
        Map<String, Integer> palette = new HashMap<>();
        Colls.distinct(Colls.map(block -> block.getBlockData().getAsString(), blocks))
                .forEach(datum -> palette.put(datum, palette.size()));

        // offsets
        List<Location> locations = Colls.map(Block::getLocation, blocks);
        Location min = Colls.reduce(locations, Locations::min);

        Map<String, String[]> offsets = new HashMap<>();

        for (Location loc : locations) {
            BlockData data = loc.getBlock().getBlockData();

            String state = null;

            // check special states
            if (data instanceof Sign) {
                state = new StateSign().serialize(data);
            } else if (data instanceof Skull) {
                state = new StateSkull().serialize(data);
            } else if (data instanceof CreatureSpawner) {
                state = new StateCreatureSpawner().serialize(data);
            }

            offsets.put(loc.subtract(min).toVector().toString(), new String[]{palette.get(data.getAsString()).toString(), state});
        }

        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(palette);
            stream.writeObject(offsets);
            stream.flush();
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save schematic %s".formatted(file), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public Schematic2 read(File file) {
        try (ObjectInputStream stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            Map<String, Integer> palette = (Map<String, Integer>) stream.readObject();
            Map<String, String[]> offsets = (Map<String, String[]>) stream.readObject();

        } catch (IOException | ClassNotFoundException ex) {
            IP.logging().stack("Error while trying to read schematic %s".formatted(file), ex);
        }

        return null;
    }
}