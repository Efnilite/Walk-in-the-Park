package dev.efnilite.ip.api.event;

import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.event.EventWrapper;
import dev.efnilite.vilib.schematic.Schematic;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourSchematicGenerateEvent extends EventWrapper {

    public final Schematic schematic;
    public final ParkourGenerator generator;
    public final ParkourPlayer2 player;

    public ParkourSchematicGenerateEvent(Schematic schematic, ParkourGenerator generator, ParkourPlayer2 player) {
        this.schematic = schematic;
        this.generator = generator;
        this.player = player;
    }
}
