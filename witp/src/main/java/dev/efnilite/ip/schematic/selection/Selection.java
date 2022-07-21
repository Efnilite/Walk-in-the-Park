package dev.efnilite.ip.schematic.selection;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A 3D rectangle selection.
 *
 * Taken from: Efnilite/Redaktor
 *
 * @see Dimensions
 */
public class Selection {

    /**
     * The world
     */
    private final World world;

    /**
     * The first position
     */
    private final Location pos1;

    /**
     * The second position
     */
    private final Location pos2;

    /**
     * Create a new CuboidSelection instance
     * <p>
     * Because this has no world, it will get the world of the locations.
     * If the world of position 1 is not set, it will get the world of the second position.
     *
     * @param   pos1
     *          The first position of the selection
     *
     * @param   pos2
     *          The second position of the selection
     */
    public Selection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.world = pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld();
    }

    /**
     * The preferred Selection constructor
     *
     * @param   pos1
     *          The first position of the selection
     *
     * @param   pos2
     *          The second position of the selection
     *
     * @param   world
     *          The world the cuboid is in
     */
    public Selection(Location pos1, Location pos2, World world) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.world = world;
    }

    /**
     * Checks the completeness of this selection.
     * If both positions are not null, this returns true.
     *
     * @return true if all variables are set, false if not
     */
    public boolean isComplete() {
        return pos1 != null && pos2 != null && world != null;
    }

    /**
     * Shifts the selection.
     * Sets the minimum location and auto adjusts the max location
     * to the same as this selection instance
     *
     * @param   min
     *          The min location
     *
     * @return  the new cuboid instance
     */
    public Selection move(Location min) {
        return new Selection(min, this.getMaximumPoint().add(min));
    }

    /**
     * Calculate the maximal point
     *
     * @return the maximal point
     */
    public Location getMaximumPoint() {
        return new Location(world,
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    /**
     * Calculate the minimal point
     *
     * @return the minimal point
     */
    public Location getMinimumPoint() {
        return new Location(world,
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    /**
     * Get the dimensions of this selection
     *
     * @return the dimensions
     */
    public Dimensions getDimensions() {
        return new Dimensions(pos1, pos2);
    }

    @Override
    public String toString() {
        return "Selection{" + "world=" + world + ", pos1=" + pos1 + ", pos2=" + pos2 + '}';
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public World getWorld() {
        return world;
    }
}