package dev.efnilite.ip.schematic;

import org.bukkit.util.Vector;

public class VectorUtil {

    public static Vector parseVector(String vector) {
        String[] split = vector.replaceAll("[()]", "").split(",");
        return new Vector(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }

    public static Vector rotateAround(Vector vector, SchematicAdjuster.RotationAngle rotation) {
        return switch (rotation) {
            case ANGLE_0 -> vector;
            case ANGLE_90 -> swapXZ(vector.multiply(new Vector(-1, 1, 1)));
            case ANGLE_180 -> vector.multiply(new Vector(-1, 1, -1));
            case ANGLE_270 -> swapXZ(vector.multiply(new Vector(1, 1, -1)));
        };
    }

    // without swapping
    public static Vector defaultRotate(Vector vector, SchematicAdjuster.RotationAngle rotation) {
        return switch (rotation) {
            case ANGLE_0 -> vector;
            case ANGLE_90 -> swapXZ(vector.multiply(new Vector(1, 1, -1)));
            case ANGLE_180 -> vector.multiply(new Vector(-1, 1, -1));
            case ANGLE_270 -> swapXZ(vector.multiply(new Vector(-1, 1, 1)));
        };
    }

    public static Vector swapXZ(Vector vector) {
        double x = vector.getX();
        vector.setX(vector.getZ());
        vector.setZ(x);
        return vector;
    }
}
