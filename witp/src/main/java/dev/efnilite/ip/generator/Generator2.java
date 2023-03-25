package dev.efnilite.ip.generator;

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
    public final Profile profile;

    /**
     * This generator's {@link Session}.
     */
    public final Session session;

    public Generator2(Session session) {
        this.session = session;

        this.profile = new Profile();
    }

    //   1
    //   1 2
    //   1 2 3
    // o 1 2 3 4 = dz 4 - dx   dy = 0
    //   1 2 3
    //   1 2
    //   1
}