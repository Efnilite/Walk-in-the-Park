package dev.efnilite.ip.schematic.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
     * Pastes a schematic at angles rotation with the provided map of offsets and BlockData.
     *
     * @param location      The smallest location.
     * @param rotation      The rotation where x = roll in rad, y = yaw in rad and z = pitch in rad.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, Vector rotation, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> {
            double[] constants = getRotationConstants(rotation);

            return Colls.thread(vectorDataMap).mapk((vector, v) -> {
                double x = vector.getX();
                double y = vector.getY();
                double z = vector.getZ();

                double newX = constants[0] * x + constants[1] * y + constants[2] * z;
                double newY = constants[3] * x + constants[4] * y + constants[5] * z;
                double newZ = constants[6] * x + constants[7] * y + constants[8] * z;

                return location.clone().add(newX, newY, newZ).getBlock();
            }).get();
        });
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