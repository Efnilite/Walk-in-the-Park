package dev.efnilite.witp.events;

import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.wrapper.EventWrapper;
import org.bukkit.block.Block;

/**
 * When a block gets generated
 */
public class BlockGenerateEvent extends EventWrapper {

    public Block block;
    public ParkourGenerator generator;
    public ParkourPlayer player;

    public BlockGenerateEvent(Block block, ParkourGenerator generator, ParkourPlayer player) {
        this.block = block;
        this.generator = generator;
        this.player = player;
    }
}
