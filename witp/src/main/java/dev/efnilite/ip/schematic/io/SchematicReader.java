package dev.efnilite.ip.schematic.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.state.State;
import dev.efnilite.ip.util.Colls;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.Map;

/**
 * Schematic reading handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicReader {

    /**
     * @param file The file.
     * @return A new {@link Schematic} instance based on the read blocks.
     */
    @SuppressWarnings("unchecked")
    public Map<Vector, BlockData> read(File file) {
        try (ObjectInputStream stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            var palette = (Map<String, Integer>) stream.readObject();
            var offsets = (Map<String, Object[]>) stream.readObject();

            Map<Integer, BlockData> paletteRef = Colls.thread(palette).inverse().mapv((k, ov) -> {
                try {
                    return Bukkit.createBlockData(ov);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }).get();

            // create final map by parse Map<String, Object> -> Vector and applying possible State

            return Colls.thread(offsets).mapkv(this::fromString, v -> {
                BlockData data = paletteRef.get((int) v[0]);

                if (v.length == 1) {
                    return data;
                }

                State state = State.getState(data);
                String extra = (String) v[1];

                return state != null ? state.deserialize(data, extra) : data;
            }).get();
        } catch (IOException | ClassNotFoundException ex) {
            IP.logging().stack("Error while trying to read schematic %s".formatted(file), ex);
        }

        return null;
    }

    private Vector fromString(String string) {
        String[] parts = string.split(",");
        return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }
}
