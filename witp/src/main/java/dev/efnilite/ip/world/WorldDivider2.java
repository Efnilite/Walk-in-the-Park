package dev.efnilite.ip.world;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldDivider2 {

    public static final Map<Integer, Session> SECTIONS = new HashMap<>();

    /**
     * Associates a session to a specific section.
     *
     * @param session The session.
     */
    public static void associate(Session session) {
        // attempts to get the closest available section to the center
        int n = 0;

        while (SECTIONS.get(n) != null) {
            n++;
        }

        SECTIONS.put(n, session);
    }

    /**
     * Disassociates a session from a specific section.
     *
     * @param session The session.
     */
    public static void disassociate(Session session) {
        SECTIONS.remove(getSectionId(session));
    }

    // returns the id of a section based on the session instance
    private static int getSectionId(Session session) {
        List<Map.Entry<Integer, Session>> filtered = Colls.filter(set -> set.getValue() == session, new ArrayList<>(SECTIONS.entrySet()));

        if (filtered.size() == 0) {
            return -1;
        } else {
            return filtered.get(0).getKey();
        }
    }

    /**
     * Returns location at the center of section n.
     *
     * @param n The section.
     * @return The location at the center.
     */
    public static Location toLocation(int n) {
        int[] coords = Util.spiralAt(n);

        return new Location(WorldManager.getWorld(),
                coords[0] * Option.BORDER_SIZE,
                (Option.MAX_Y + Option.MIN_Y) / 2.0,
                coords[1] * Option.BORDER_SIZE);
    }

    /**
     * Returns the entire selection of section n.
     *
     * @param n The section.
     * @return The location at the center.
     */
    public static Selection toSelection(int n) {
        Location center = toLocation(n);

        // get the min and max locations
        Location max = center.clone().add(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2);
        Location min = center.clone().subtract(Option.BORDER_SIZE / 2, 0, Option.BORDER_SIZE / 2);

        max.setY(Option.MAX_Y);
        min.setY(Option.MIN_Y);

        return new Selection(max, min);
    }
}