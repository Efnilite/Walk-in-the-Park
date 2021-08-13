package dev.efnilite.witp.schematic;

import org.bukkit.util.Vector;

public class Vector3D {

    public int x;
    public int y;
    public int z;

    public Vector3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D rotateAround(RotationAngle rotation) {
        switch (rotation) {
            case ANGLE_0:
                return this;
            case ANGLE_90:
                return multiply(-1, 1).swapXZ();
            case ANGLE_180:
                return multiply(-1, -1);
            case ANGLE_270:
                return multiply(1, -1).swapXZ();
            default:
                throw new IllegalArgumentException();
        }
    }

    // without swapping
    public Vector3D defaultRotate(RotationAngle rotation) {
        switch (rotation) {
            case ANGLE_0:
                return this;
            case ANGLE_90:
                return multiply(1, -1).swapXZ();
            case ANGLE_180:
                return multiply(-1, -1);
            case ANGLE_270:
                return multiply(-1, 1).swapXZ();
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector toBukkitVector() {
        return new Vector(x, y, z);
    }

    public static Vector3D fromBukkit(Vector vector) {
        return new Vector3D(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public Vector3D clone() {
        return new Vector3D(x, y, z);
    }

    public Vector3D subtract(Vector3D vector) {
        this.x -= vector.x;
        this.y -= vector.y;
        this.z -= vector.z;
        return this;
    }

    public Vector3D swapXZ() {
        int x = this.x;
        this.x = this.z;
        this.z = x;
        return this;
    }

    public Vector3D multiply(double x, double z) {
        this.x *= x;
        this.z *= z;
        return this;
    }

    public Vector3D setX(int x) {
        this.x = x;
        return this;
    }

    public Vector3D setY(int y) {
        this.y = y;
        return this;
    }

    public Vector3D setZ(int z) {
        this.z = z;
        return this;
    }

    public Vector3D abs() {
        this.x = Math.abs(x);
        this.y = Math.abs(y);
        this.z = Math.abs(z);
        return this;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    public enum RotationAngle {
        ANGLE_0(180),
        ANGLE_90(270),
        ANGLE_180(0),
        ANGLE_270(90);

        private final int opposite;

        RotationAngle(int opposite) {
            this.opposite = opposite;
        }

        public static RotationAngle getFromInteger(int angle) {
            return valueOf("ANGLE_" + angle);
        }

        public int getOpposite() {
            return opposite;
        }
    }
}