package dev.efnilite.witp.schematic.getter;

import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A class for getting blocks async in a cube.
 *
 * Taken from: Efnilite/Redaktor
 */
public class AsyncBlockGetter extends BukkitRunnable implements AsyncGetter<List<Block>> {

    /**
     * The selection
     */
    private Selection selection;

    /**
     * The consumer -> what to do when the blocks have been collected
     */
    private Consumer<List<Block>> consumer;

    /**
     * Creates a new instance
     *
     * @param   selection
     *          The selection
     *
     * @param   consumer
     *          What to do when the blocks have been gathered
     */
    public AsyncBlockGetter(Selection selection, Consumer<List<Block>> consumer) {
        this.selection = selection;
        this.consumer = consumer;

        Tasks.asyncTask(this);
    }

    @Override
    public void run() {
        if (selection != null) {
            Selection cuboid = selection;
            List<Block> blocks = new ArrayList<>();
            Location max = cuboid.getMaximumPoint();
            Location min = cuboid.getMinimumPoint();
            Location loc = Util.zero();
            loc.setWorld(selection.getWorld());
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        loc.setX(x);
                        loc.setY(y);
                        loc.setZ(z);

                        blocks.add(loc.getBlock());
                    }
                }
            }
            asyncDone(blocks);
        }
    }

    @Override
    public void asyncDone(List<Block> value) {
        consumer.accept(value);
    }
}