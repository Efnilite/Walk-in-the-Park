package dev.efnilite.ip.generator;

import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import org.bukkit.util.Vector;

/**
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since v5.0.0
 */
public class Generator2 {

    /**
     * The current heading of the parkour.
     */
    public Vector heading;

    /**
     * This generator's {@link Profile}.
     */
    public final Profile profile = new Profile();

    /**
     * This generator's {@link Session}.
     */
    public final Session session;

    /**
     * This generator's {@link Island} manager.
     */
    public final Island island;

    public Generator2(Session session) {
        this.session = session;

        this.island = new Island(session, Schematics.CACHE.get("spawn-island.witp"));
    }
}