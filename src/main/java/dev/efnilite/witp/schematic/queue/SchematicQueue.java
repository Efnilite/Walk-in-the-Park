package dev.efnilite.witp.schematic.queue;

import dev.efnilite.witp.schematic.SchematicReader;
import dev.efnilite.witp.schematic.selection.Dimensions;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A queue for pasting schematics
 *
 * Taken from: Efnilite/Redaktor
 */
public class SchematicQueue implements EditQueue<SchematicReader.ReaderReturn> {

    /**
     * The first position
     */
    private Location pos;

    /**
     * Creates a new instance
     *
     * @param   pos
     *          The first position
     */
    public SchematicQueue(Location pos) {
        this.pos = pos;
    }

    @Override
    public void build(SchematicReader.ReaderReturn readerReturn) {
        Dimensions dimensions = readerReturn.getDimensions();
        List<BlockData> data = readerReturn.getData();

        List<BlockMap> blocks = new ArrayList<>(); // Temp use of BlockMap
        Location max = Util.max(dimensions.getSelection().move(pos).getMaximumPoint(), pos);
        Location min = Util.min(dimensions.getSelection().move(pos).getMaximumPoint(), pos);
        Location loc = min.clone();
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                int index = 0;
                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                    for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                            loc.setX(x);
                            loc.setY(y);
                            loc.setZ(z);

                            blocks.add(new BlockMap(loc.getBlock(), data.get(index)));
                            index++;
                        }
                    }
                }
                asyncDone(blocks);
            }
        };
        Tasks.asyncTask(runnable);
    }

    public void asyncDone(List<BlockMap> map) {
        CopyQueue queue = new CopyQueue();
        queue.build(map);
    }
}