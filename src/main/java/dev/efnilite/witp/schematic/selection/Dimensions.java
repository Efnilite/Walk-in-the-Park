package dev.efnilite.witp.schematic.selection;

import dev.efnilite.witp.util.Util;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * The dimensions of a CuboidSelection.
 *
 * Taken from: Efnilite/Redaktor
 *
 * @see Selection
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
        this.maximum = Util.max(pos1, pos2);
        this.minumum = Util.min(pos1, pos2);

        Location max = this.maximum;
        Location min = this.minumum;

        this.width = max.getBlockX() - min.getBlockX() + 1;
        this.height = max.getBlockY() - min.getBlockY() + 1;
        this.length = max.getBlockZ() - min.getBlockZ() + 1;
    }

    public Dimensions(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    /**
     * Calculates the volume
     *
     * @return the volume
     */
    public int getVolume() {
        return width * height * length;
    }

    /**
     * Gets the width
     *
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height
     *
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the length
     *
     * @return the length
     */
    public int getLength() {
        return length;
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

    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    @Override
    public String toString() {
        return "(" + width + "," + height + "," + length + ")"; // x, y, z
    }
}