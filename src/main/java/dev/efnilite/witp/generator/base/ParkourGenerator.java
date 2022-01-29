package dev.efnilite.witp.generator.base;

import dev.efnilite.witp.generator.Stopwatch;
import dev.efnilite.witp.generator.subarea.Direction;
import dev.efnilite.witp.generator.subarea.SubareaPoint;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main class for Parkour Generation handling
 */
public abstract class ParkourGenerator {

    /**
     * Generator options
     */
    public List<GeneratorOption> generatorOptions;

    /**
     * The time of the player's current session
     *
     * @see Stopwatch#toString()
     */
    public String time = "0.0s";

    /**
     * The direction of the parkour
     */
    public Direction heading;

    /**
     * The score of the player
     */
    public int score;

    /**
     * At which range the direction of the parkour will change for players.
     */
    protected int borderWarning = 50;

    public SubareaPoint.Data data;

    /**
     * List of this player's spectators
     */
    public final HashMap<String, ParkourSpectator> spectators;
    protected final Stopwatch stopwatch;
    protected final double borderOffset;

    /**
     * The player associated with this Generator.
     */
    protected final ParkourPlayer player;

    /**
     * The random for this Thread, which is useful in randomly generating parkour
     */
    protected final Random random;

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
     * Implementable method for selecting materials used to set the blocks used in {@link #selectBlocks()}
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
     * Method that gets run every game tick (every 2 ticks)
     */
    public abstract void tick();

    /**
     * Generates a block or blocks
     */
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
}