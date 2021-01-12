package dev.efnilite.witp.generator;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.Configuration;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.util.Vector;

public abstract class ParkourGenerator {

    /**
     * The heading of the parkour
     */
    public Vector heading;
    public final double borderOffset;
    protected final Stopwatch stopwatch;
    protected final ParkourPlayer player;

    public ParkourGenerator(ParkourPlayer player) {
        this.player = player;
        this.stopwatch = new Stopwatch();
        this.borderOffset = Configuration.Option.BORDER_SIZE / 2.0;
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

}
