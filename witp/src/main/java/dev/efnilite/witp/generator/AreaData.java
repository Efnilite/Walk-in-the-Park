package dev.efnilite.witp.generator;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.List;

public class AreaData {

    public List<Chunk> spawnChunks;
    public List<Block> blocks;

    public AreaData(List<Block> blocks, List<Chunk> spawnChunks) {
        this.blocks = blocks;
        this.spawnChunks = spawnChunks;
    }
}