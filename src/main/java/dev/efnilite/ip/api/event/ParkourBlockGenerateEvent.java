package dev.efnilite.ip.api.event;

import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.event.EventWrapper;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourBlockGenerateEvent extends EventWrapper {

    public final List<Block> blocks;
    public final ParkourGenerator generator;
    public final ParkourPlayer2 player;

    public ParkourBlockGenerateEvent(List<Block> blocks, ParkourGenerator generator, ParkourPlayer2 player) {
        this.blocks = blocks;
        this.generator = generator;
        this.player = player;
    }
}
