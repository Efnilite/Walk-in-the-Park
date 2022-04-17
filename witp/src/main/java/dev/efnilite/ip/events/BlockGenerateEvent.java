package dev.efnilite.ip.events;

import dev.efnilite.vilib.event.EventWrapper;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
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
