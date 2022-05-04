package dev.efnilite.ip.session;

import dev.efnilite.ip.player.data.Score;

import java.util.UUID;

/**
 * Interface for Tournament handling.
 * Only one Tournament can take place at the same time.
 */
public interface Tournament {

    /**
     * Adds a score to the registered scores, used in the leaderboard.
     *
     * @param   uuid
     *          The uuid of the player
     *
     * @param   score
     *          The {@link Score} instance belonging to the player's finished run.
     */
    void addScore(UUID uuid, Score score);

    /**
     * Sets the current active tournament
     *
     * @param   tournament
     *          The new instance of a class that implements Tournament
     */
    static void setActive(Tournament tournament) {
        Manager.setActive(tournament);
    }

    /**
     * Gets the current active tournament
     *
     * @return the current active tournament
     */
    static Tournament getActive() {
        return Manager.getActive();
    }

    /**
     * Returns whether there is currently an active tournament.
     *
     * @return true if there is, false if not.
     */
    static boolean isActive() {
        return Manager.getActive() != null;
    }

    class Manager {

        private static Tournament active;

        public static void setActive(Tournament tournament) {
            active = tournament;
        }

        public static Tournament getActive() {
            return active;
        }
    }
}