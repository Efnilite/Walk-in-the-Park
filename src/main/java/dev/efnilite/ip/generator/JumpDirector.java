package dev.efnilite.ip.generator;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * The JumpDirector provides calculations to ensure parkour blocks stay within the allowed playable area.
 */
public class JumpDirector {

    private static final int SAFE_DISTANCE = 5;
    private final BoundingBox bb;
    private final double[][] progress;

    /**
     * Constructor.
     *
     * @param bb    The bounding box of the allowed parkour area.
     * @param point The point to base the direction on.
     */
    public JumpDirector(@NotNull BoundingBox bb, @NotNull Vector point) {
        this.bb = bb;
        this.progress = calculateParameterization(point);
    }

    /**
     * Calculates the player's position in a parameter form, to make it easier to detect when the player is near the edge of the border.
     * Returns a 2-dimensional array where the first array index is used to select the x, y and z (0, 1 and 2 respectively).
     *
     * @return Array where the first index is tx and second index is borderMarginX (see comments in code for explanation).
     */
    private double[][] calculateParameterization(@NotNull Vector point) {
        Vector min = bb.getMin();
        Vector max = bb.getMax();

        // the total dimensions
        double dx = max.getX() - min.getX();
        double dy = max.getY() - min.getY();
        double dz = max.getZ() - min.getZ();

        // the relative x, y and z coordinates
        // relative being from the min point of the selection zone
        double relativeX = point.getX() - min.getX();
        double relativeY = point.getY() - min.getY();
        double relativeZ = point.getZ() - min.getZ();

        // get progress along axes
        // tx = 0 means that the player is at the same x coordinate as the min point (origin)
        // tx = 1 means that the player is at the same x coordinate as the max point
        // everything between is the progress between these two points, relatively speaking
        double tx = relativeX / dx;
        double ty = relativeY / dy;
        double tz = relativeZ / dz;

        // the margin until the border
        // if tx < borderMarginX, it means the x coordinate is within 'safeDistance' blocks of the border
        double borderMarginX = SAFE_DISTANCE / dx;
        double borderMarginY = SAFE_DISTANCE / dy;
        double borderMarginZ = SAFE_DISTANCE / dz;

        return new double[][]{{tx, borderMarginX}, {ty, borderMarginY}, {tz, borderMarginZ}};
    }

    /**
     * Updates the heading to make sure it avoids the border of the selected zone.
     * When the most recent block is detected to be within a 5-block radius of the border,
     * the heading will automatically be turned around to ensure that the edge does not get
     * destroyed.
     *
     * @return The recommended new heading. Current heading if no modification is needed.
     */
    @NotNull
    public Vector getRecommendedHeading(Vector current) {
        // get x values from progress array
        double tx = progress[0][0];
        double borderMarginX = progress[0][1];

        // get z values from progress array
        double tz = progress[2][0];
        double borderMarginZ = progress[2][1];

        Vector recommendedHeading = new Vector(0, 0, 0);
        // check border
        if (tx < borderMarginX) {
            // x should increase
            recommendedHeading = new Vector(1, 0, 1);
        } else if (tx > 1 - borderMarginX) {
            // x should decrease
            recommendedHeading = new Vector(-1, 0, -1);
        }

        if (tz < borderMarginZ) {
            // z should increase
            recommendedHeading = new Vector(1, 0, 1);
        } else if (tz > 1 - borderMarginZ) {
            // z should decrease
            recommendedHeading = new Vector(-1, 0, -1);
        }

        if (recommendedHeading.isZero()) {
            return current;
        } else {
            return recommendedHeading;
        }
    }

    /**
     * Updates the height to make sure the player doesn't go below the playable zone.
     * If the current height is fine, it will return 0.
     * If the current height is within the border margin, it will return a value (1 or -1)
     * to make sure the player doesn't go below this value
     *
     * @return The recommended new height. 0 if no modification is needed.
     */
    public int getRecommendedHeight(int current) {
        double ty = progress[1][0];
        double borderMarginY = progress[1][1];

        if (ty < borderMarginY) {
            // y should increase
            return 1;
        } else if (ty > 1 - borderMarginY) {
            // y should decrease
            return -1;
        }
        return current;
    }

    /**
     * @return The progress array.
     * @see #calculateParameterization(Vector)
     */
    public double[][] getProgress() {
        return progress;
    }
}