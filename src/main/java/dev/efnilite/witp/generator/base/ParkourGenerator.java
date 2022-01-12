package dev.efnilite.witp.generator.base;

import dev.efnilite.witp.generator.Stopwatch;
import dev.efnilite.witp.generator.subarea.SubareaPoint;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.schematic.Vector3D;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class ParkourGenerator {

    // -= Generator Options =-
    public List<GeneratorOption> generatorOptions;

    /**
     * The time of the player's current session
     *
     * @see Stopwatch#toString()
     */
    public String time = "0.0s";

    /**
     * The heading of the parkour
     */
    public Vector3D heading;

    /**
     * The score of the player
     */
    public int score;

    /**
     * At which range the direction of the parkour will change for players.
     */
    protected int borderWarning = 50;

    public SubareaPoint.Data data;
    public final HashMap<String, ParkourSpectator> spectators;
    protected final double borderOffset;
    protected final Stopwatch stopwatch;
    protected final ParkourPlayer player;

    public ParkourGenerator(ParkourPlayer player, GeneratorOption... options) {
        this.player = player;
        this.generatorOptions = Arrays.asList(options);
        this.stopwatch = new Stopwatch();
        this.spectators = new HashMap<>();
        this.borderOffset = Option.BORDER_SIZE.get() / 2.0;
        player.setGenerator(this);
    }

    public abstract void reset(boolean regenerateBack);

    public abstract void start();

    public abstract void generate();

    public abstract void menu();

    protected abstract static class InventoryHandler {

        protected final ParkourPlayer pp;
        protected final Player player;

        public InventoryHandler(ParkourPlayer pp) {
            this.pp = pp;
            this.player = pp.getPlayer();
        }
    }

    // whether the option is present but simplified
    protected boolean option(GeneratorOption option) {
        return generatorOptions.contains(option);
    }

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