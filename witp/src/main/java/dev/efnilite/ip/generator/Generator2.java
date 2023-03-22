package dev.efnilite.ip.generator;

import dev.efnilite.ip.generator.profile.Profile;
import dev.efnilite.ip.session.Session2;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>Iteration 2.</p>
 * @author Efnilite
 * @since v5.0.0
 */
public class Generator2 {

    /**
     * This generator's {@link Profile}.
     */
    public final Profile profile;

    /**
     * This generator's {@link Session2}.
     */
    public final Session2 session2;

    // a random to make it easier to access
    protected final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Generator2(Session2 session2) {
        this.session2 = session2;

        this.profile = new Profile();
    }
}
