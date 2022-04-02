package dev.efnilite.witp.generator.base;

import dev.efnilite.witp.generator.AreaData;
import dev.efnilite.witp.player.ParkourPlayer;
import org.bukkit.Location;

/**
 * An intermediary class to reduce the amount of clutter in the {@link dev.efnilite.witp.generator.DefaultGenerator} class.
 *
 */
public abstract class DefaultGeneratorBase extends DefaultGeneratorChances {

    /**
     * The total score achieved in this Generator instance
     */
    protected int totalScore;

    /**
     * The schematic cooldown
     */
    protected int schematicCooldown;

    /**
     * Where the player spawns on reset
     */
    protected Location playerSpawn;

    /**
     * Where blocks from schematics spawn
     */
    protected Location blockSpawn;

    /**
     * Area data
     */
    protected AreaData data;

    public DefaultGeneratorBase(ParkourPlayer player, GeneratorOption... generatorOptions) {
        super(player, generatorOptions);
    }

    /**
     * If a specified location is nearing or outside the playable area.
     *
     * @param   location
     *          The location to check
     *
     * @return true if the location is closer than the border warning, false if not.
     */
    public boolean isNearingEdge(Location location) {
        double[] distances = zone.distanceToAxes(location);

        return distances[0] < 50 || distances[2] < 50;
    }

    public AreaData getData() {
        return data;
    }

    public void setData(AreaData data) {
        this.data = data;
    }
}