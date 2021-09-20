package dev.efnilite.witp.generator;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.generator.subarea.SubareaPoint;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.util.Vector;

import java.util.HashMap;

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
    public final HashMap<String, ParkourSpectator> spectators;
    protected final double borderOffset;
    protected final Stopwatch stopwatch;
    protected final ParkourPlayer player;

    public ParkourGenerator(ParkourPlayer player) {
        this.player = player;
        this.stopwatch = new Stopwatch();
        this.spectators = new HashMap<>();
        this.borderOffset = Option.BORDER_SIZE / 2.0;
        this.heading = WITP.getDivider().getHeading();
        player.setGenerator(this);
    }

    public abstract void reset(boolean regenerateBack);

    public abstract void start();

    public abstract void generate();

    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            this.spectators.remove(spectator.getPlayer().getName());
        }
    }

    public void addSpectator(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            this.spectators.put(spectator.getPlayer().getName(), spectator);
        }
    }

    /**
     * Updates the stats for spectators
     */
    public void updateSpectators() {
        for (ParkourSpectator spectator : spectators.values()) {
            spectator.checkDistance();
            spectator.updateScoreboard();
        }
    }

    public ParkourPlayer getPlayer() {
        return player;
    }

    /**
     * Updates the time
     */
    public void updateTime() {
        time = stopwatch.toString();
    }

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