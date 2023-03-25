package dev.efnilite.ip.world;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Divides the parkour world in sections, each with an active session.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since v5.0.0
 */
public class WorldDivider {

    public static final Map<Integer, Session> SESSIONS = new HashMap<>();

    /**
     * Associates a session to a specific section.
     *
     * @param session The session.
     */
    public static void associate(Session session) {
        // attempts to get the closest available section to the center
        int n = 0;

        while (SESSIONS.get(n) != null) {
            n++;
        }

        SESSIONS.put(n, session);
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void disassociate(Session session) {
        SESSIONS.remove(getSectionId(session));
    }

    /**
     * @param session The session.
     * @return The location at the center of section n.
     */
    public static Location toLocation(Session session) {
        int[] coords = Util.spiralAt(getSectionId(session));

        return new Location(WorldManager.getWorld(),
                coords[0] * Option.BORDER_SIZE,
                (Option.MAX_Y + Option.MIN_Y) / 2.0,
                coords[1] * Option.BORDER_SIZE);
    }


    // returns the section id from the session instance
    private static int getSectionId(Session session) {
        List<Map.Entry<Integer, Session>> filtered = Colls.filter(set -> set.getValue() == session, new ArrayList<>(SESSIONS.entrySet()));

        if (filtered.size() == 0) {
            return -1;
        } else {
            return filtered.get(0).getKey();
        }
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