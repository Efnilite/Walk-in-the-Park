package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Divides the parkour world in sections, each with an active session.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class WorldDivider {

    /**
     * Map with all session ids map to the session instances.
     */
    public static final Map<Integer, Session> sessions = new HashMap<>();

    /**
     * Associates a session to a specific section.
     *
     * @param session The session.
     */
    public static synchronized void associate(Session session) {
        // attempts to get the closest available section to the center
        int n = 0;

        while (sessions.containsKey(n)) {
            n++;
        }

        sessions.put(n, session);

        IP.log("Added session at %s".formatted(toLocation(session).toVector()));
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void disassociate(Session session) {
        sessions.remove(getSectionId(session));
    }

    /**
     * @param session The session.
     * @return The location at the center of section n.
     */
    public static Location toLocation(Session session) {
        int[] xz = Util.spiralAt(getSectionId(session));

        return new Location(WorldManager.getWorld(),
                xz[0] * Option.BORDER_SIZE,
                (Option.MAX_Y + Option.MIN_Y) / 2.0,
                xz[1] * Option.BORDER_SIZE);
    }


    // returns the section id from the session instance. error if no found.
    private static int getSectionId(Session session) {
        return sessions.entrySet().stream()
                .filter(entry -> entry.getValue() == session)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
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

        return new Location[] { min, max };
    }
}