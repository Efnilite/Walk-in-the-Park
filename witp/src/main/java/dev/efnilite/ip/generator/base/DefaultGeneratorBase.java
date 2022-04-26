package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.generator.AreaData;
import dev.efnilite.ip.session.Session;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * An intermediary class to reduce the amount of clutter in the {@link dev.efnilite.ip.generator.DefaultGenerator} class.
 *
 */
public abstract class DefaultGeneratorBase extends DefaultGeneratorChances {

    /**
     * The total score achieved in this Generator instance
     */
    protected int totalScore = 0;

    /**
     * The schematic cooldown
     */
    protected int schematicCooldown = 20;

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

    public DefaultGeneratorBase(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);
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