package dev.efnilite.witp.generator;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class ChunkLoader {

    private List<Block> blocks = new ArrayList<>();

    public void updateFor(Chunk chunk) {
        Block toUpdate = chunk.getBlock(0, chunk.getWorld().getMaxHeight(), 0);
        toUpdate.setType(Material.BARRIER);
        blocks.add(toUpdate);
    }

    public void removeAll() {
        for (Block block : blocks) {
            block.setType(Material.AIR);
        }
    }
}