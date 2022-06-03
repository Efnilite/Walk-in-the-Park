package dev.efnilite.ip.generator;

import dev.efnilite.vilib.vector.Vector3D;

public enum Direction {

    NORTH(new Vector3D(0, 0, -1), 90),
    EAST(new Vector3D(1, 0, 0), 0),
    SOUTH(new Vector3D(0, 0, 1), 270),
    WEST(new Vector3D(-1, 0, 0), 180);

    private final int angleFromBase;
    private final Vector3D vector;

    Direction(Vector3D vector, int angleFromBase) {
        this.vector = vector;
        this.angleFromBase = angleFromBase;
    }

    public int getAngleFromBase() {
        return angleFromBase;
    }

    public Vector3D toVector() {
        return vector;
    }
}