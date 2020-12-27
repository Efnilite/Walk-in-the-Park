package dev.efnilite.witp.generator.subarea;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a 2D Vector -> (x, z)
 *
 * @author Efnilite
 */
public class SubareaPoint {

    public int x;
    public int z;

    public SubareaPoint(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Vector getEstimatedCenter(double borderSize) {
        int size = (int) borderSize;
        return new Vector(x * size, 150, z * size);
    }

    public SubareaPoint zero() {
        this.x = 0;
        this.z = 0;
        return this;
    }

    public SubareaPoint add(SubareaPoint point) {
        this.x += point.x;
        this.z += point.z;
        return this;
    }

    public SubareaPoint subtract(SubareaPoint point) {
        this.x -= point.x;
        this.z -= point.z;
        return this;
    }

    /**
     * Gets the SubareaPoints between 2 points
     *
     * @param   other
     *          The other point
     *
     * @return the points inbetween these 2 points (including these points)
     */
    public List<SubareaPoint> getInBetween(SubareaPoint other) {
        List<SubareaPoint> points = new ArrayList<>();
        if (other.z == z) { // For example: this is (1,1) and other is (-1,1), z will be the same
            int deltaX = other.x - x; // -1 - 1 = -2
            int abs = Math.abs(deltaX); //  -(-2) = 2
            int increment = deltaX / abs; // -2 / 2 = -1, so every loop the x gets decreased by 1
            int current = x; // = 1
            for (int i = 0; i <= abs; i++) {
                points.add(new SubareaPoint(current, z));
                current += increment;
            }
        } else if (other.x == x) {
            int deltaZ = other.z - z;
            int abs = Math.abs(deltaZ);
            int increment = deltaZ / abs;
            int current = z;
            for (int i = 0; i <= abs; i++) {
                points.add(new SubareaPoint(x, current));
                current += increment;
            }
        } else {
            throw new IllegalArgumentException("X or Z must be the same if getting the points in a straight line");
        }
        return points;
    }

    public boolean equals(SubareaPoint other) {
        return other.x == x && other.z == z;
    }

    @Override
    public String toString() {
        return "SubareaPoint{" + "x=" + x + ", z=" + z + '}';
    }

    /**
     * Data for SubareaPoints - used to delete the spawn island once the player leaves
     */
    public static class Data {

        public List<Location> blocks;

        public Data(List<Location> blocks) {
            this.blocks = blocks;
        }
    }
}
