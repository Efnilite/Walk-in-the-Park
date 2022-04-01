package dev.efnilite.witp.generator.base;

import dev.efnilite.witp.generator.AreaData;
import dev.efnilite.witp.generator.Direction;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.util.Stopwatch;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main class for Parkour Generation handling
 */
public abstract class ParkourGenerator {

    /**
     * The score of the player
     */
    protected int score;

    /**
     * At which range the direction of the parkour will change for players.
     */
    protected int borderWarning = 50;

    /**
     * Area data
     */
    protected AreaData data;

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
     * The offset of the border
     */
    protected final double borderOffset;

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
    protected final ParkourPlayer player;

    /**
     * The spectators
     */
    protected final Map<UUID, ParkourSpectator> spectators;

    public ParkourGenerator(ParkourPlayer player, GeneratorOption... options) {
        this.player = player;
        this.generatorOptions = Arrays.asList(options);
        this.stopwatch = new Stopwatch();
        this.spectators = new HashMap<>();
        this.borderOffset = Option.BORDER_SIZE.get() / 2.0;
        this.random = ThreadLocalRandom.current();

        player.setGenerator(this);
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
     * If the vector is near the border
     *
     * @param vector The vector
     */
    public boolean isNearBorder(Vector vector) {
        Vector xBorder = vector.clone();
        Vector zBorder = vector.clone();

        xBorder.setX(borderOffset);
        zBorder.setZ(borderOffset);

        return vector.distance(xBorder) < borderWarning || vector.distance(zBorder) < borderWarning;
    }

    /**
     * Updates the stopwatch time and visual time for the player
     */
    public void updateTime() {
        time = stopwatch.toString();

        player.updateVisualTime(player.selectedTime);
    }

    /**
     * Gets the owning player
     *
     * @return the owning player
     */
    public ParkourPlayer getPlayer() {
        return player;
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
     * Gets the border offset
     *
     * @return the border offset
     */
    public double getBorderOffset() {
        return borderOffset;
    }

    /**
     * Gets the current time
     *
     * @return the player's current time
     */
    public String getTime() {
        return time;
    }

    public void setData(AreaData data) {
        this.data = data;
    }

    public AreaData getData() {
        return data;
    }
}