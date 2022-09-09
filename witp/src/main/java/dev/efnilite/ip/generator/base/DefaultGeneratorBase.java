package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.data.AreaData;
import dev.efnilite.ip.generator.settings.GeneratorOption;
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
    public int totalScore = 0;

    /**
     * The schematic cooldown
     */
    public int schematicCooldown = Option.SCHEMATIC_COOLDOWN;

    /**
     * Where the player spawns on reset
     */
    public Location playerSpawn;

    /**
     * Where blocks from schematics spawn
     */
    public Location blockSpawn;

    /**
     * Area data
     */
    public AreaData data;

    public DefaultGeneratorBase(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);
    }

    public AreaData getData() {
        return data;
    }

    public void setData(AreaData data) {
        this.data = data;
    }
}