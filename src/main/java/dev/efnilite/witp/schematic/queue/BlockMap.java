package dev.efnilite.witp.schematic.queue;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class BlockMap {

    private Block block;
    private BlockData data;

    public BlockMap(Block block) {
        this.block = block;
        this.data = block.getBlockData();
    }

    public BlockMap(Block block, BlockData data) {
        this.block = block;
        this.data = data;
    }

    public BlockData getData() {
        return data;
    }

    public Block getBlock() {
        return block;
    }
}