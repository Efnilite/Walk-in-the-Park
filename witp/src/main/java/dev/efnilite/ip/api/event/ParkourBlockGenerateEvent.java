package dev.efnilite.ip.api.event;

import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;
import org.bukkit.block.Block;

/**
 * Gets called when a block is generated. Read-only.
 */
public class ParkourBlockGenerateEvent extends EventWrapper {

    public final Block block;
    public final DefaultGenerator generator;
    public final ParkourPlayer player;

    public ParkourBlockGenerateEvent(Block block, DefaultGenerator generator, ParkourPlayer player) {
        this.block = block;
        this.generator = generator;
        this.player = player;
    }
}
