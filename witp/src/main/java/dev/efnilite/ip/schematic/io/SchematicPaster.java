package dev.efnilite.ip.schematic.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Schematic pasting handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicPaster {

    /**
     * Pastes a schematic with the provided map of offsets and BlockData.
     *
     * @param location      The smallest location.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> Colls.thread(vectorDataMap)
                .mapk((k, v) -> location.clone().add(k).getBlock())
                .get());
    }

    /**
     * Pastes a schematic at angle rotation with the provided map of offsets and BlockData.
     * Rotates {@link Directional} blocks.
     *
     * @param location      The smallest location.
     * @param rotation      The rotation where y is the yaw in rad.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, double rotation, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> Colls.thread(vectorDataMap)
            .mapkv((vector) -> location.clone().add(round(vector.rotateAroundY(rotation))).getBlock(),
                (data) -> {
                    if (data instanceof Directional directional) {
                        Vector direction = directional.getFacing().getDirection();
                        Vector rotated = round(direction.rotateAroundY(rotation));
                        directional.setFacing(getClosestMatchingBlockFace(directional, rotated));
                    }

                    return data;
                })
            .get());
    }

    // rounds a vector to avoid small discrepancies like having an offset of E-16
    private Vector round(Vector vector) {
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        double epsilon = Vector.getEpsilon();

        return new Vector(Math.abs(x) >= epsilon ? x : 0,
                Math.abs(y) >= epsilon ? y : 0,
                Math.abs(z) >= epsilon ? z : 0);
    }

    // returns an available BlockFace that matches the closest to the rotated direction of the block
    private BlockFace getClosestMatchingBlockFace(Directional directional, Vector rotated) {
        return directional.getFaces().stream()
                .min(Comparator.comparingDouble(f -> f.getDirection().angle(rotated)))
                .orElseThrow();
    }

    private List<Block> paste(Supplier<Map<Block, BlockData>> blocksGetter) {
        try {
            Map<Block, BlockData> blocks = CompletableFuture.supplyAsync(blocksGetter).get();

            blocks.forEach((block, data) -> block.setBlockData(data, false));

            return new ArrayList<>(blocks.keySet());
        } catch (InterruptedException | ExecutionException ex) {
            IP.logging().stack("Error while trying to get blocks of schematic", ex);

            return Collections.emptyList();
        }
    }
}