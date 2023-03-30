package dev.efnilite.ip.schematic.v2;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.v2.io.SchematicPaster;
import dev.efnilite.ip.schematic.v2.io.SchematicReader;
import dev.efnilite.ip.schematic.v2.io.SchematicWriter;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Schematic2 {

    /**
     * @return A new schematic bulider.
     */
    public static Builder create() {
        return new Builder();
    }

    static class Builder {

        /**
         * Loads a schematic.
         *
         * @param file The file.
         * @return The builder for loaded schematics.
         */
        public BuilderLoaded load(File file) {
            return new BuilderLoaded(file);
        }

        /**
         * Loads a schematic.
         *
         * @param file The file.
         * @return The builder for loaded schematics.
         */
        public BuilderLoaded load(String file) {
            return new BuilderLoaded(new File(file));
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

    /**
     * Builder for loaded schematics.
     */
    static class BuilderLoaded {

        private Map<Vector, BlockData> vectorBlockMap;

        public BuilderLoaded(File file) {
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
        public void paste(Location location) {
            new SchematicPaster().paste(location, vectorBlockMap);
        }

        /**
         * Pastes a schematic at angles rotation.
         *
         * @param location The smallest location.
         * @param rotation The rotation.
         */
        public void paste(Location location, Vector rotation) {
            new SchematicPaster().paste(location, rotation, vectorBlockMap);
        }

        /**
         * @return The map of vectors mapped to each {@link BlockData}.
         */
        public Map<Vector, BlockData> getVectorBlockMap() {
            return vectorBlockMap;
        }
    }
}
