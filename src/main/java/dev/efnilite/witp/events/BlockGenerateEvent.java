package dev.efnilite.witp.events;

import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.wrapper.EventWrapper;
import org.bukkit.block.Block;

/**
 * When a block gets generated
 */
public class BlockGenerateEvent extends EventWrapper {

    public final Block block;
    public final DefaultGenerator generator;
    public final ParkourPlayer player;

    public BlockGenerateEvent(Block block, DefaultGenerator generator, ParkourPlayer player) {
        this.block = block;
        this.generator = generator;
        this.player = player;
    }
}
