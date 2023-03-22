package dev.efnilite.ip.session;

import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.world.WorldDivider2;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * <p>A session is bound to a {@link dev.efnilite.ip.world.WorldDivider} section.
 * It manages all players, all spectators, visibility, the generator, etc.</p>
 * <p>Iteration 2.</p>
 * @author Efnilite
 * @since v5.0.0
 */
public class Session2 {

    /**
     * The generator.
     */
    public ParkourGenerator generator;

    /**
     * The visibility of this session. Default public.
     */
    public Visibility visibility = Visibility.PUBLIC;

    /**
     * Function that takes the current session and returns whether new players should be accepted.
     */
    public Function<Session2, Boolean> isAcceptingPlayers = session -> false;

    /**
     * Function that takes the current session and returns whether new spectators should be accepted.
     */
    public Function<Session2, Boolean> isAcceptingSpectators = session -> session.visibility == Visibility.PUBLIC;

    /**
     * List of muted users.
     */
    public final List<ParkourUser> muted = new ArrayList<>();

    /**
     * List of players.
     */
    protected final Map<UUID, ParkourPlayer> players = new HashMap<>();

    /**
     * List of spectators.
     */
    protected final Map<UUID, ParkourSpectator> spectators = new HashMap<>();

    private Session2() { }

    /**
     * Adds provided players to this session's player list.
     *
     * @param players The players to add.
     */
    // todo add join message
    public void addPlayers(ParkourPlayer... players) {
        for (ParkourPlayer player : players) {
            this.players.put(player.getUUID(), player);
        }
    }

    /**
     * Removes provided players from this session's player list.
     *
     * @param players The players to remove.
     */
    public void removePlayers(ParkourPlayer... players) {
        for (ParkourPlayer player : players) {
            this.players.remove(player.getUUID());
        }
    }

    /**
     * Returns this session's players.
     *
     * @return The players.
     */
    public List<ParkourPlayer> getPlayers() {
        return new ArrayList<>(players.values());
    }

    /**
     * Adds provided spectators to this session's spectator list.
     *
     * @param spectators The spectators to add.
     */
    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.sendTranslated("play.spectator.other_join", spectator.getName());
            }

            this.spectators.put(spectator.getUUID(), spectator);
        }
    }

    /**
     * Removes provided spectators from this session's spectator list.
     *
     * @param spectators The spectators to remove.
     */
    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.sendTranslated("play.spectator.other_leave", spectator.getName());
            }

            this.spectators.remove(spectator.getUUID());
        }
    }

    /**
     * Returns this session's spectators.
     *
     * @return The spectators.
     */
    public List<ParkourSpectator> getSpectators() {
        return new ArrayList<>(spectators.values());
    }

    /**
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void mute(@NotNull ParkourUser user) {
        if (!muted.remove(user)) {
            muted.add(user);
        }
    }

    public enum Visibility {

        /**
         * No-one can join.
         */
        PRIVATE,

        /**
         * Only people with the session id can join.
         */
        ID_ONLY,

        /**
         * Anyone can join.
         */
        PUBLIC,

    }

    /**
     * Helper class for constructing a session.
     */
    public static class Builder {

        private Function<Session2, Boolean> isAcceptingPlayers;
        private Function<Session2, Boolean> isAcceptingSpectators;
        private ParkourPlayer[] players;

        /**
         * Creates a new builder.
         *
         * @return A new builder instance.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Sets the accepting players function.
         * @see Session2#isAcceptingPlayers
         *
         * @param f The function.
         * @return This instance.
         */
        public Builder isAcceptingPlayers(Function<Session2, Boolean> f) {
            isAcceptingPlayers = f;

            return this;
        }

        /**
         * Sets the accepting spectators function.
         * @see Session2#isAcceptingSpectators
         *
         * @param f The function.
         * @return This instance.
         */
        public Builder isAcceptingSpectators(Function<Session2, Boolean> f) {
            isAcceptingSpectators = f;

            return this;
        }

        /**
         * Adds initial players.
         * @see Session2#players
         *
         * @param p The players.
         * @return This instance.
         */
        public Builder addPlayers(ParkourPlayer... p) {
            players = p;

            return this;
        }

        /**
         * Builds a new session instance with the provided settings.
         * Assigns the session with {@link WorldDivider2#associate(Session2)}.
         *
         * @return The constructed session.
         */
        public Session2 complete() {
            Session2 session = new Session2();

            if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
            if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;
            if (players != null) session.addPlayers(players);

            WorldDivider2.associate(session);

            return session;
        }
    }
}