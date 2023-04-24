package dev.efnilite.ip.session;

import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.world.WorldDivider;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * <p>A session is bound to a {@link WorldDivider} section.
 * It manages all players, all spectators, visibility, the generator, etc.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Session {

    /**
     * Creates a new builder.
     *
     * @return A new builder instance.
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * The generator.
     */
//   todo public ParkourGenerator generator;

    /**
     * The visibility of this session. Default public.
     */
    public Visibility visibility = Visibility.PUBLIC;

    /**
     * Function that takes the current session and returns whether new players should be accepted.
     */
    public Function<Session, Boolean> isAcceptingPlayers = session -> false;

    /**
     * Function that takes the current session and returns whether new spectators should be accepted.
     */
    public Function<Session, Boolean> isAcceptingSpectators = session -> session.visibility == Visibility.PUBLIC;

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

    private Session() { }

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

        if (players.length > 0 && this.players.size() == 0) {
            players[0].generator.reset(false);
            WorldDivider.disassociate(this);
        }
    }

    /**
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

        private Function<Session, Boolean> isAcceptingPlayers;
        private Function<Session, Boolean> isAcceptingSpectators;
        private ParkourPlayer[] players;
        private Function<Session, ParkourGenerator> generator;

        /**
         * Sets the accepting players function.
         *
         * @param f The function.
         * @return This instance.
         * @see Session#isAcceptingPlayers
         */
        public Builder isAcceptingPlayers(Function<Session, Boolean> f) {
            isAcceptingPlayers = f;

            return this;
        }

        /**
         * Sets the accepting spectators function.
         *
         * @param f The function.
         * @return This instance.
         * @see Session#isAcceptingSpectators
         */
        public Builder isAcceptingSpectators(Function<Session, Boolean> f) {
            isAcceptingSpectators = f;

            return this;
        }

        /**
         * Adds initial players.
         *
         * @param p The players.
         * @return This instance.
         * @see Session#players
         */
        public Builder addPlayers(ParkourPlayer... p) {
            players = p;

            return this;
        }

        /**
         * Adds the generator.
         *
         * @param f The generator.
         * @return This instance.
         */
        // todo move to generator?
        public Builder generator(Function<Session, ParkourGenerator> f) {
            generator = f;

            return this;
        }

        /**
         * Builds a new session instance with the provided settings.
         * Assigns the session with {@link WorldDivider#associate(Session)}.
         *
         * @return The constructed session.
         */
        public Session complete() {
            Session session = new Session();

            if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
            if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;
            if (players != null) session.addPlayers(players);

            WorldDivider.associate(session);

            // todo move to generator?
            Arrays.asList(players).forEach(p -> {
                p.session = session;
                p.generator = generator.apply(session);

                p.updateGeneratorSettings();

                p.generator.island.build();
            });
            return session;
        }
    }
}