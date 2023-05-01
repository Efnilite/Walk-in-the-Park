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

import static java.lang.Math.cos;
import static java.lang.Math.sin;

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
     * @param rotation      The rotation where x = roll in rad, y = yaw in rad and z = pitch in rad.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, Vector rotation, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> {
            double[] constants = getRotationConstants(rotation);

            return Colls.thread(vectorDataMap).mapkv(
                    (vector) -> location.clone().add(rotate(vector, constants)).getBlock(),
                    (data) -> {
                        if (data instanceof Directional directional) {
                            Vector direction = directional.getFacing().getDirection();
                            Vector rotated = rotate(direction, constants);
                            directional.setFacing(getClosestMatchingBlockFace(directional, rotated));
                        }

                        return data;
                    }
            ).get();
        });
    }

    // returns an available BlockFace that matches the closest to the rotated direction of the block
    // todo fix
    private BlockFace getClosestMatchingBlockFace(Directional directional, Vector rotated) {
        return directional.getFaces().stream()
                .min(Comparator.comparingDouble(f -> Math.abs(f.getDirection().angle(rotated))))
                .orElseThrow();
    }

    private Vector rotate(Vector vector, double[] rotationConstants) {
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();

        double newX = rotationConstants[0] * x + rotationConstants[1] * y + rotationConstants[2] * z;
        double newY = rotationConstants[3] * x + rotationConstants[4] * y + rotationConstants[5] * z;
        double newZ = rotationConstants[6] * x + rotationConstants[7] * y + rotationConstants[8] * z;

        return new Vector(newX, newY, newZ);
    }

    // 3d rotation matrix constants, calculate once instead of however many blocks we have times
    private double[] getRotationConstants(Vector rotation) {
        // rotation in roll pitch yaw
        // matrix in yaw (a) pitch (b) roll (y)
        double a = rotation.getZ();
        double b = rotation.getY();
        double y = rotation.getX();

        // src https://en.wikipedia.org/wiki/Rotation_matrix#General_rotations
        return new double[] {
                cos(a) * cos(b),    cos(a) * sin(b) * sin(y) - sin(a) * cos(y),     cos(a) * sin(b) * cos(y) + sin(a) * sin(y),
                sin(a) * cos(b),    sin(a) * sin(b) * sin(y) + cos(a) * cos(y),     sin(a) * sin(b) * cos(y) - cos(a) * sin(y),
                -sin(b),            cos(b) * sin(y),                                cos(b) * cos(y)};
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