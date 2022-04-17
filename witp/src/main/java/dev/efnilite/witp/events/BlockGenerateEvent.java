package dev.efnilite.witp.events;

import dev.efnilite.vilib.event.EventWrapper;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import org.bukkit.block.Block;

/**
 * Event that gets called when a block generates in Parkour.
 * This event is read-only.
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
