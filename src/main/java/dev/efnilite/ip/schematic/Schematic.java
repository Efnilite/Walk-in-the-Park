package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.io.SchematicPaster;
import dev.efnilite.ip.schematic.io.SchematicReader;
import dev.efnilite.ip.schematic.io.SchematicWriter;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Main schematic handling class.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Schematic {

    /**
     * The version of this library instance.
     */
    public static final int VERSION = 1;

    /**
     * @return A new schematic builder.
     */
    public static Builder create() {
        return new Builder();
    }

    private final File file;
    private Map<Vector, BlockData> vectorBlockMap;

    /**
     * Constructor.
     *
     * @param file The file.
     */
    public Schematic(@NotNull File file) {
        this.file = file;

        try {
            this.vectorBlockMap = CompletableFuture.supplyAsync(() -> new SchematicReader().read(file)).get();
        } catch (InterruptedException | ExecutionException ex) {
            IP.logging().stack("Error while trying to read schematic %s".formatted(file), ex);
        }
    }

    /**
     * Pastes a schematic.
     *
     * @param location The smallest location.
     */
    public List<Block> paste(Location location) {
        return new SchematicPaster().paste(location, vectorBlockMap);
    }

    /**
     * Pastes a schematic at angles rotation.
     *
     * @param location The smallest location.
     * @param rotation The rotation where y = yaw in rad.
     */
    public List<Block> paste(Location location, double rotation) {
        return new SchematicPaster().paste(location, rotation, vectorBlockMap);
    }

    /**
     * @return The dimensions of this schematic.
     */
    public Vector getDimensions() {
        Set<Vector> offsets = vectorBlockMap.keySet();

        Vector min = offsets.stream().reduce((a, b) -> new Vector(min(a.getX(), b.getX()), min(a.getY(), b.getY()), min(a.getZ(), b.getZ()))).orElseThrow();
        Vector max = offsets.stream().reduce((a, b) -> new Vector(max(a.getX(), b.getX()), max(a.getY(), b.getY()), max(a.getZ(), b.getZ()))).orElseThrow();

        return max.subtract(min);
    }

    /**
     * @return True when this schematic contains no unknown {@link BlockData}, false if it does.
     */
    public boolean isSupported() {
        return !vectorBlockMap.containsValue(null);
    }

    /**
     * @return The map of vectors mapped to each {@link BlockData}.
     */
    public Map<Vector, BlockData> getVectorBlockMap() {
        return vectorBlockMap;
    }

    /**
     * @return The file.
     */
    public File getFile() {
        return file;
    }

    public static class Builder {

        /**
         * Loads a schematic.
         *
         * @param file The file.
         * @return A new {@link Schematic} instance.
         */
        public Schematic load(File file) {
            return new Schematic(file);
        }

        /**
         * Loads a schematic.
         *
         * @param file The file.
         * @return A new {@link Schematic} instance.
         */
        public Schematic load(String file) {
            return new Schematic(new File(file));
        }

        /**
         * Saves the selection between the two locations asynchronously to file.
         *
         * @param file The file.
         * @param pos1 The first position.
         * @param pos2 The second position.
         */
        public void save(String file, Location pos1, Location pos2) {
            save(new File(file), pos1, pos2);
        }

        /**
         * Saves the selection between the two locations asynchronously to file.
         *
         * @param file The file.
         * @param pos1 The first position.
         * @param pos2 The second position.
         */
        public void save(File file, Location pos1, Location pos2) {
            Task.create(IP.getPlugin()).async().execute(() -> new SchematicWriter().save(file, pos1, pos2)).run();
        }
    }
}
