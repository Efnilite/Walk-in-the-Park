package dev.efnilite.ip.world;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
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
    public static synchronized void add(Session session) {
        // attempts to get the closest available section to the center
        var missing = IntStream.range(0, sections.size() + 1)
                .filter(i -> !sections.containsValue(i))
                .findFirst()
                .orElseThrow();

        sections.put(session, missing);
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void remove(Session session) {
        sections.remove(session);
    }

    /**
     * @param session The session.
     * @return The location at the center of section n.
     */
    public static Location toLocation(Session session) {
        int[] xz = Util.spiralAt(sections.get(session));

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
}