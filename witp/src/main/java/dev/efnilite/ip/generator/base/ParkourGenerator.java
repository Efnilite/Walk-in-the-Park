package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.generator.GeneratorOption;
import dev.efnilite.ip.generator.profile.Profile;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Stopwatch;
import dev.efnilite.vilib.vector.Vector3D;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main class for Parkour Generation handling
 */
public abstract class ParkourGenerator {

    /**
     * This generator's score
     */
    public int score = 0;

    /**
     * The zone in which the parkour can take place. (playable area)
     */
    public Location[] zone;

    /**
     * The direction of the parkour
     */
    public Vector3D heading;

    /**
     * Generator options
     */
    public List<GeneratorOption> generatorOptions;

    /**
     * The {@link Stopwatch} instance
     */
    public final Stopwatch stopwatch;

    /**
     * The {@link Session} associated with this Generator.
     */
    public final Session session;

    /**
     * This Generator's {@link Profile}.
     *
     * @see Profile
     */
    public final Profile profile;

    /**
     * The random for this Thread, which is useful in randomly generating parkour
     */
    protected final ThreadLocalRandom random;

    public ParkourGenerator(@NotNull Session session, GeneratorOption... options) {
        this.session = session;
        this.profile = new Profile();

        this.generatorOptions = Arrays.asList(options);
        this.stopwatch = new Stopwatch();
        this.random = ThreadLocalRandom.current();
    }

    /**
     * Gets the {@link Gamemode} associated with this Generator.
     *
     * @return the {@link Gamemode}
     */
    public abstract Gamemode getGamemode();

    /**
     * Defines the way this generator's (and accompanying gamemode) scoreboard will look.
     * The code in this method has no restrictions and may apply to anyone.
     */
    public abstract void updateScoreboard();

    /**
     * A method that gets called when the instance is applied to the player.
     * This is used internally to update the Generator's {@link Profile}
     * with settings that should override the player's selection.
     */
    public abstract void updatePreferences();

    /**
     * Applies particles to list of blocks
     *
     * @param   applyTo
     *          The blocks to apply the particles to.
     */
    public abstract void particles(List<Block> applyTo);

    /**
     * Implementable method for selecting materials used to set the blocks used in {@link #selectBlocks()}.
     */
    public abstract BlockData selectBlockData();

    /**
     * Implementable method for selecting blocks that are to-be set
     */
    public abstract List<Block> selectBlocks();

    /**
     * Implementable method for what happens when a player scores
     */
    public abstract void score();

    /**
     * Implementable method for what happens when a player scores
     */
    public abstract void fall();

    /**
     * Implementable method for menu handling
     */
    public abstract void menu();

    /**
     * Method for resetting the parkour
     *
     * @param   regenerateBack
     *          Whether the parkour should regenerate back or not
     */
    public abstract void reset(boolean regenerateBack);

    /**
     * Starts the tick method
     */
    public abstract void startTick();

    /**
     * Method that gets run every game tick
     */
    public abstract void tick();

    /**
     * Generates a block or blocks
     */
    public abstract void generate();

    /**
     * Updates the stopwatch time and visual time for the player
     */
    public void updateTime() {
        session.getPlayers().forEach(player -> player.updateVisualTime(player.selectedTime));
    }

    /**
     * Gets the score of the player
     *
     * @return the score
     */
    public int getScore() {
        return score;
    }
}