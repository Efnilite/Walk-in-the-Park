package dev.efnilite.ip.generator.base;

import dev.efnilite.vilib.vector.Vector3D;

public class Direction {

    public static Vector3D NORTH = new Vector3D(0, 0, -1); //(new Vector3D(0, 0, -1), 90),
    public static Vector3D EAST = new Vector3D(1, 0, 0); //(new Vector3D(1, 0, 0), 0),
    public static Vector3D SOUTH = new Vector3D(0, 0, 1); //(new Vector3D(0, 0, 1), 270),
    public static Vector3D WEST = new Vector3D(-1, 0, 0); // (new Vector3D(-1, 0, 0), 180);

    public static Vector3D translate(String direction) {
        return switch (direction.toLowerCase()) {
            case "north" -> new Vector3D(0, 0, -1);
            case "south" -> new Vector3D(0, 0, 1);
            case "west" -> new Vector3D(-1, 0, 0);
            default -> new Vector3D(1, 0, 0); // east or others
        };
    }
}