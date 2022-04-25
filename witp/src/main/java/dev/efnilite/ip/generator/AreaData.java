package dev.efnilite.ip.generator;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.List;

public record AreaData(List<Block> blocks, List<Chunk> spawnChunks) {

}