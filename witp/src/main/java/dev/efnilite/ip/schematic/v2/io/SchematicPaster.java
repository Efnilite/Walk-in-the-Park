package dev.efnilite.ip.schematic.v2.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Map;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class SchematicPaster {

    /**
     * Pastes a schematic with the provided map of offsets and BlockData.
     *
     * @param location      The smallest location.
     * @param vectorDataMap The map.
     */
    public void paste(Location location, Map<Vector, BlockData> vectorDataMap) {
        Runnable runnable = () -> vectorDataMap.forEach((vector, blockData) -> location.clone()
                .add(vector)
                .getBlock()
                .setBlockData(blockData, false));

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Task.create(IP.getPlugin()).execute(runnable).run();
        }
    }

    /**
     * Pastes a schematic at angles rotation with the provided map of offsets and BlockData.
     *
     * @param location      The smallest location.
     * @param rotation      The rotation.
     * @param vectorDataMap The map.
     */
    public void paste(Location location, Vector rotation, Map<Vector, BlockData> vectorDataMap) {
        double[] constants = getRotationConstants(rotation);

        Map<Vector, BlockData> newMap = Colls.mapk((vector, v) -> {
            double x = vector.getX();
            double y = vector.getY();
            double z = vector.getZ();

            double newX = constants[0] * x + constants[1] * y + constants[2] * z;
            double newY = constants[3] * x + constants[4] * y + constants[5] * z;
            double newZ = constants[6] * x + constants[7] * y + constants[8] * z;

            return new Vector(newX, newY, newZ);
        }, vectorDataMap);

        paste(location, newMap);
    }

    // 3d rotation matrix constants, calculate once instead of however many blocks we have times
    private double[] getRotationConstants(Vector rotation) {
        double a = rotation.getX();
        double b = rotation.getY();
        double y = rotation.getZ();

        // src https://en.wikipedia.org/wiki/Rotation_matrix#General_rotations
        return new double[]{cos(a) * cos(b), cos(a) * sin(b) * sin(y) - sin(a) * cos(y), cos(a) * sin(b) * cos(y) + sin(a) * sin(y), sin(a) * cos(b), sin(a) * sin(b) * sin(y) + cos(a) * cos(y), sin(a) * sin(b) * cos(y) - cos(a) * sin(y), -sin(b), cos(b) * sin(y), cos(b) * cos(y)};
    }
}