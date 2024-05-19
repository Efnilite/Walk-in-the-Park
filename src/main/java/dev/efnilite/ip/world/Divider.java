package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.session.Session;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * <p>Divides the parkour world in sections, each with an active session.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Divider {

    /**
     * Map with all session ids map to the session instances.
     */
    public static final Map<Session, Integer> sections = new HashMap<>();

    /**
     * Associates a session to a specific section.
     *
     * @param session The session.
     */
    public static synchronized Vector add(Session session) {
        // attempts to get the closest available section to the center
        var missing = IntStream.range(0, sections.size() + 1)
                .filter(i -> !sections.containsValue(i))
                .findFirst()
                .orElseThrow();

        sections.put(session, missing);

        var vector = toVector(session);

        IP.log("Added session at %s".formatted(vector));

        return vector;
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void remove(Session session) {
        IP.log("Removed session at %s".formatted(toVector(session)));

        sections.remove(session);
    }

    /**
     * @param session The session.
     * @return The location at the center of section n.
     */
    public static Vector toVector(Session session) {
        int[] xz = spiralAt(sections.get(session));

        return new Vector(xz[0] * Option.BORDER_SIZE,
                (Option.MAX_Y + Option.MIN_Y) / 2.0,
                xz[1] * Option.BORDER_SIZE);
    }

    /**
     * @param session The session.
     * @return Array where the first item is the smallest location and second item is the largest.
     */
    public static Vector[] toSelection(Session session) {
        var center = toVector(session);

        // get the min and max locations
        var max = center.clone().add(new Vector(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2));
        var min = center.clone().subtract(new Vector(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2));

        max.setY(Option.MAX_Y);
        min.setY(Option.MIN_Y);

        return new Vector[]{min, max};
    }

    /**
     * Gets a spiral
     *
     * @param n The number of  value
     * @return the coords of this value
     */
    // https://math.stackexchange.com/a/163101
    private static int[] spiralAt(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Invalid n bound: %d".formatted(n));
        }

        n++; // one-index
        int k = (int) Math.ceil((Math.sqrt(n) - 1) / 2);
        int t = 2 * k + 1;
        int m = t * t;
        t--;

        if (n > m - t) {
            return new int[]{k - (m - n), -k};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k, -k + (m - n)};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k + (m - n), k};
        } else {
            return new int[]{k, k - (m - n - t)};
        }
    }
}