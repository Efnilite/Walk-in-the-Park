package dev.efnilite.witp.schematic.queue;

import java.util.List;

/**
 * A queue for setting a lot of blocks to specific materials.
 */
public class CopyQueue implements EditQueue<List<BlockMap>> {

    @Override
    public void build(List<BlockMap> map) {
        for (BlockMap blockMap : map) {
            if (!blockMap.getData().getAsString().equals(blockMap.getBlock().getBlockData().getAsString())) {
                blockMap.getBlock().setBlockData(blockMap.getData());
            }
        }
    }
}