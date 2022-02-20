package dev.efnilite.witp.generator;

import dev.efnilite.witp.WITP;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to prevent generation delay, which for an unknown reason started in version 2.9,
 * after overhauling the DefaultGenerator.
 */
public class ChunkLoader {

    private final Map<Chunk, Block> blocks = new HashMap<>();

    public void updateRadius(Chunk base, int radius) {
        for (Chunk chunk : WITP.getDivider().getChunksAround(base, radius)) {
            updateSpecific(chunk);
        }
    }

    public void updateSpecific(Chunk chunk) {
        if (blocks.containsKey(chunk)) {
            return;
        }

        chunk.load();

        Block toUpdate = chunk.getBlock(0, chunk.getWorld().getMaxHeight() - 1, 0);
        toUpdate.setType(Material.BARRIER, true);
        blocks.put(chunk, toUpdate);
    }

    public void removeAll() {
        for (Block block : blocks.values()) {
            block.setType(Material.AIR);
        }
        blocks.clear();
    }
}