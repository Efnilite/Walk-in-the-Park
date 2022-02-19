package dev.efnilite.witp.session;

import com.google.common.annotations.Beta;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default methods for a playing session.
 * This class stores data about a playing session, since a player can change class several types while
 * switching between gamemodes. This class may include multiple players, or just one.
 *
 * @since v3.0.3
 * @author Efnilite
 */
@Beta
public interface Session {

    /**
     * Checks the distance between spectators and players.
     */
    void updateSpectators();

    /**
     * Adds spectator(s).
     *
     * @param   spectators
     *          The spectator(s) to be added.
     */
    void addSpectators(ParkourSpectator... spectators);

    /**
     * Removes spectator(s).
     *
     * @param   spectators
     *          The spectator(s) to be removed.
     */
    void removeSpectators(ParkourSpectator... spectators);

    /**
     * Whether this Session is accepting spectators
     *
     * @return true if it is, false if not.
     */
    boolean isAcceptingSpectators();

    /**
     * Gets a list of all spectators
     *
     * @return the spectators
     */
    @NotNull List<ParkourSpectator> getSpectators();

    /**
     * Adds player(s).
     *
     * @param   players
     *          The player(s) to be added.
     */
    void addPlayers(ParkourPlayer... players);

    /**
     * Removes player(s).
     *
     * @param   players
     *          The player(s) to be removed.
     */
    void removePlayers(ParkourPlayer... players);

    /**
     * Whether this Session is accepting players
     *
     * @return true if it is, false if not.
     */
    boolean isAcceptingPlayers();

    /**
     * Gets a list of all the players in this Session.
     *
     * @return a list of all {@link ParkourPlayer}s in this session
     */
    @NotNull List<ParkourPlayer> getPlayers();

    /**
     * Returns the id unique to this Session.
     *
     * @return the Session id.
     */
    @NotNull String getSessionId();

    /**
     * Automatically assigns an unregistered player to this session, based
     * on the return values of {@link #isAcceptingPlayers()} and {@link #isAcceptingSpectators()}.
     * This is done very discretely. If you want to add your own checks for spectators, etc.,
     * overriding this method is most effective.
     *
     * @param   player
     *          The player to force to join
     */
    default void join(Player player) {
        if (isAcceptingPlayers()) {
            addPlayers(new ParkourPlayer(player, null));
        } else if (isAcceptingSpectators()) {
//            addSpectators(new ParkourSpectator(player, null, null)); todo
        }
    }

    /**
     * Generates this Session's id.
     *
     * @return a 6 character long id. Example: A40SDF, D4AXHD, ICJXXX, VIBCXZ, etc.
     */
    default @NotNull String generateSessionId() {
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(chars[ThreadLocalRandom.current().nextInt(chars.length)]);
        }
        String finalId = builder.toString();
        if (getSession(finalId) != null) { // prevent duplicates
            return generateSessionId();
        }

        return builder.toString();
    }

    /**
     * Creates a new player session.
     *
     * @param   player
     *          The ParkourPlayer to associate this new session with.
     *
     * @return the created instance.
     */
    static @NotNull Session createSession(@NotNull ParkourPlayer player) {
        Session session = new SingleSession();
        session.addPlayers(player);

        Manager.put(player.getUUID(), session); // update session pool
        return session;
    }

    /**
     * Closes a session associated with an id.
     *
     * @param   uuid
     *          The id of the session.
     */
    static void removeSession(@NotNull UUID uuid) {
        Session session = getSession(uuid);
        if (session == null) {
            return;
        }

        if (session.getPlayers().size() == 1 && session.getSpectators().size() == 0) { // if there are no other players/spectators, close session
            closeSession(session.getSessionId());
        }

        Manager.remove(uuid);
    }

    /**
     * Closes a session associated with an id.
     *
     * @param   id
     *          The id of the session.
     */
    static void closeSession(String id) {
        Manager.close(id);
    }

    /**
     * Returns a Session based on the Session's id.
     *
     * @param   id
     *          The id. A 6-character string.
     *
     * @return the Session instance. Null if not found.
     */
    static @Nullable Session getSession(String id) {
        return Manager.getSession(id);
    }

    /**
     * Returns the session a player is currently in. UUID-based.
     *
     * @param   uuid
     *          The uuid of the player. A 6-character string.
     *
     * @return the Session instance. Null if not found.
     */
    static @Nullable Session getSession(UUID uuid) {
        return Manager.getSession(Manager.getId(uuid));
    }

    /**
     * Manager class for all sessions
     */
    @ApiStatus.Internal
    class Manager {

        private static final Map<UUID, String> sessionIds = new HashMap<>();
        private static final Map<String, Session> sessions = new HashMap<>();

        static void put(UUID uuid, Session session) {
            String id = session.getSessionId();

            sessions.put(id, session);
            sessionIds.put(uuid, id);
        }

        static void remove(UUID uuid) {
            sessionIds.remove(uuid);
        }

        static void close(String id) {
            sessions.remove(id);
        }

        // Gets the ID of a session using a player's uuid
        static String getId(UUID uuid) {
            return sessionIds.get(uuid);
        }

        // Gets the session using the internal ID
        static Session getSession(String id) {
            return sessions.get(id);
        }
    }
}