package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.util.Locations;
import org.bukkit.Location;

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
    public static synchronized Location add(Session session) {
        // attempts to get the closest available section to the center
        var missing = IntStream.range(0, sections.size() + 1)
                .filter(i -> !sections.containsValue(i))
                .findFirst()
                .orElseThrow();

        sections.put(session, missing);

        var location = toLocation(session);

        IP.log("Added session at %s".formatted(Locations.toString(location, true)));

        return location;
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void remove(Session session) {
        IP.log("Removed session at %s".formatted(Locations.toString(toLocation(session), true)));

        sections.remove(session);
    }

    /**
     * @param session The session.
     * @return The location at the center of section n.
     */
    private static Location toLocation(Session session) {
        int[] xz = spiralAt(sections.get(session));

        return new Location(WorldManager.getWorld(),
                xz[0] * Option.BORDER_SIZE,
                (Option.MAX_Y + Option.MIN_Y) / 2.0,
                xz[1] * Option.BORDER_SIZE);
    }

    /**
     * @param session The session.
     * @return Array where the first item is the smallest location and second item is the largest.
     */
    public static Location[] toSelection(Session session) {
        Location center = toLocation(session);

        // get the min and max locations
        Location max = center.clone().add(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2);
        Location min = center.clone().subtract(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2);

        max.setY(Option.MAX_Y);
        min.setY(Option.MIN_Y);

        return new Location[]{min, max};
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