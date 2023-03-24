package dev.efnilite.ip.schematic;

import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.vector.Vector3D;
import org.bukkit.Location;

/**
 * The dimensions of a CuboidSelection.
 * <p>
 * Taken from: Efnilite/Redaktor
 */
public class Dimensions {

    /**
     * The width
     */
    private final int width;

    /**
     * The height
     */
    private final int height;

    /**
     * The length
     */
    private final int length;

    /**
     * The max location
     */
    private Location maximum;

    /**
     * The min location
     */
    private Location minumum;

    /**
     * Creates a new instance
     */
    public Dimensions(Location pos1, Location pos2) {
        this.maximum = Locations.max(pos1, pos2);
        this.minumum = Locations.min(pos1, pos2);

        Location max = this.maximum;
        Location min = this.minumum;

        this.width = max.getBlockX() - min.getBlockX();
        this.height = max.getBlockY() - min.getBlockY();
        this.length = max.getBlockZ() - min.getBlockZ();
    }

    public Dimensions(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    /**
     * Gets the maximum point
     *
     * @return the max point
     */
    public Location getMaximumPoint() {
        return maximum;
    }

    /**
     * Gets the minimal point
     *
     * @return the minimal point
     */
    public Location getMinimumPoint() {
        return minumum;
    }

    public Vector3D toVector3D() {
        return new Vector3D(width, height, length);
    }

    @Override
    public String toString() {
        return "(" + width + "," + height + "," + length + ")"; // x, y, z
    }
}