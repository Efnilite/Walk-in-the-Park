package dev.efnilite.witp.schematic.selection;

import dev.efnilite.witp.util.Util;
import org.bukkit.Location;

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
    private int width;

    /**
     * The height
     */
    private int height;

    /**
     * The length
     */
    private int length;

    /**
     * The max location
     */
    private Location maximum;

    /**
     * The min location
     */
    private Location minumum;

    /**
     * The selection
     */
    private Selection selection;

    /**
     * Creates a new instance
     *
     * @param   selection
     *          The cuboid
     */
    public Dimensions(Selection selection) {
        this.selection = selection;
        this.maximum = selection.getMaximumPoint();
        this.minumum = selection.getMinimumPoint();

        Location max = this.maximum;
        Location min = this.minumum;

        this.width = max.getBlockX() - min.getBlockX() + 1;
        this.height = max.getBlockY() - min.getBlockY() + 1;
        this.length = max.getBlockZ() - min.getBlockZ() + 1;
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

    /**
     * Gets the selection
     *
     * @return the selection
     */
    public Selection getSelection() {
        return selection;
    }

    @Override
    public String toString() {
        return Util.toString(maximum, false) + "/" + Util.toString(minumum, false);
    }
}