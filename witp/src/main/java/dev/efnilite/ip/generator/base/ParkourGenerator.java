package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.generator.Direction;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Stopwatch;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main class for Parkour Generation handling
 */
public abstract class ParkourGenerator {

    /**
     * The score of the player
     */
    protected int score = 0;

    /**
     * The zone in which the parkour can take place.
     */
    protected Selection zone;

    /**
     * The time of the player's current session
     *
     * @see Stopwatch#toString()
     */
    protected String time = "0.0s";

    /**
     * The direction of the parkour
     */
    protected Direction heading;

    /**
     * Generator options
     */
    protected List<GeneratorOption> generatorOptions;

    /**
     * The random for this Thread, which is useful in randomly generating parkour
     */
    protected final Random random;

    /**
     * The stopwatch instance
     */
    protected final Stopwatch stopwatch;

    /**
     * The player associated with this Generator.
     */
    protected final Session session;

    /**
     * The spectators
     */
    protected final Map<UUID, ParkourSpectator> spectators;

    public ParkourGenerator(@NotNull Session session, GeneratorOption... options) {
        this.session = session;
        this.generatorOptions = Arrays.asList(options);
        this.stopwatch = new Stopwatch();
        this.spectators = new HashMap<>();
        this.random = ThreadLocalRandom.current();

        session.getPlayers().forEach(player -> player.setGenerator(this));
    }

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
        time = stopwatch.toString();

        session.getPlayers().forEach(player -> player.updateVisualTime(player.selectedTime));
    }

    /**
     * Sets the current zone of this generator.
     *
     * @param   zone
     *          The zone of this generator.
     */
    public void setZone(Selection zone) {
        this.zone = zone;
    }

    /**
     * Gets the score of the player
     *
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * Gets the current time
     *
     * @return the player's current time
     */
    public String getTime() {
        return time;
    }

    /**
     * Gets the current heading as {@link Direction}
     *
     * @return the current heading.
     */
    public Direction getHeading() {
        return heading;
    }

    /**
     * Gets the {@link Session} this Generator belongs to
     *
     * @return the {@link Session}
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the current {@link GeneratorOption}s.
     *
     * @return the list of current Generator options.
     */
    public List<GeneratorOption> getGeneratorOptions() {
        return generatorOptions;
    }
}