package dev.efnilite.witp.generator;

import dev.efnilite.witp.generator.subarea.SubareaPoint;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.Option;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class ParkourGenerator {

    /**
     * The time of the player's current session
     *
     * @see Stopwatch#toString()
     */
    public String time = "0.0s";
    /**
     * The heading of the parkour
     */
    public Vector heading;
    /**
     * The score of the player
     */
    public int score;
    public SubareaPoint.Data data;
    public final double borderOffset;
    protected final Stopwatch stopwatch;
    protected final ParkourPlayer player;

    public ParkourGenerator(ParkourPlayer player) {
        this.player = player;
        this.stopwatch = new Stopwatch();
        this.borderOffset = Option.BORDER_SIZE / 2.0;
    }

    /**
     * Updates the time
     */
    public void updateTime() {
        time = stopwatch.toString();
    }

    public abstract void generate();

    /**
     * Checks if a vector is following the assigned heading
     *
     * @param vector The direction vector between the latest spawned parkour block and a new possible block
     * @return true if the vector is following the heading assigned to param heading
     */
    public boolean isFollowing(Vector vector) {
        if (heading.getBlockZ() != 0) { // north/south
            return vector.getZ() * heading.getZ() > 0;
        } else if (heading.getBlockX() != 0) { // east/west
            return vector.getX() * heading.getX() < 0;
        } else {
            Verbose.error("Invalid heading vector: " + heading.toString());
            return false;
        }
    }

    /**
     * If the vector is near the border
     *
     * @param vector The vector
     */
    public boolean isNearBorder(Vector vector) {
        Vector xBorder = vector.clone();
        Vector zBorder = vector.clone();

        xBorder.setX(borderOffset);
        zBorder.setZ(borderOffset);

        return vector.distance(xBorder) < 75 || vector.distance(zBorder) < 75;
    }
}