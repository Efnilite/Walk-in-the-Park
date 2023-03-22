package dev.efnilite.ip.generator;

import dev.efnilite.ip.session.Session2;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

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
     * This generator's {@link Session2}.
     */
    public final Session2 session;

    // a random to make it easier to access
    protected final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Generator2(Session2 session) {
        this.session = session;

        this.profile = new Profile();
    }
}