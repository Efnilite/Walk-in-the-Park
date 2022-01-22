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

    @Override
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

    public Vector3D subtract(double x, double z) {
        this.x -= x;
        this.z -= z;
        return this;
    }

    public Vector3D add(double x, double z) {
        this.x += x;
        this.z += z;
        return this;
    }

    public Vector3D multiply(double modifier) {
        this.x *= modifier;
        this.y *= modifier;
        this.z *= modifier;
        return this;
    }

    public double distanceTo(Vector3D other) {
        double x2 = Math.pow(other.x - x, 2);
        double y2 = Math.pow(other.y - y, 2);
        double z2 = Math.pow(other.z - z, 2);
        return Math.sqrt(x2 + y2 + z2);
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

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}