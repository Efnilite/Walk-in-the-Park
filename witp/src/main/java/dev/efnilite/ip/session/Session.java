package dev.efnilite.ip.session;

import dev.efnilite.ip.config.Locales;
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
    public static Builder create(Function<Session, ParkourGenerator> generatorFunction) {
        return new Builder(generatorFunction);
    }

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
    private Function<Session, Boolean> isAcceptingPlayers = session -> false;

    /**
     * Function that takes the current session and returns whether new spectators should be accepted.
     */
    private Function<Session, Boolean> isAcceptingSpectators = session -> session.visibility == Visibility.PUBLIC;

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

    protected Session() { }

    /**
     * Adds provided players to this session's player list.
     *
     * @param players The players to add.
     */
    public void addPlayers(ParkourPlayer... players) {
        for (ParkourPlayer player : players) {
            player.session = this;

            for (ParkourPlayer to : getPlayers()) {
                to.send(Locales.getString(player.locale, "lobby.other_join").formatted(player.getName()));
            }
        }

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

        for (ParkourPlayer player : players) {
            player.session = null;

            for (ParkourPlayer to : getPlayers()) {
                to.send(Locales.getString(player.locale, "lobby.other_leave").formatted(player.getName()));
            }
        }

        if (players.length > 0 && this.players.size() == 0) {
            generator.reset(false);
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
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void mute(@NotNull ParkourUser user) {
        if (!muted.remove(user)) {
            muted.add(user);
        }
    }

    /**
     * @return True when players may join this session, false if not.
     */
    public boolean isAcceptingPlayers() {
        return isAcceptingPlayers.apply(this);
    }


    /**
     * @return True when spectators may join this session, false if not.
     */
    public boolean isAcceptingSpectators() {
        return isAcceptingSpectators.apply(this);
    }

    /**
     * @return The spectators.
     */
    public List<ParkourSpectator> getSpectators() {
        return new ArrayList<>(spectators.values());
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
        private final Function<Session, ParkourGenerator> generator;

        private Builder(Function<Session, ParkourGenerator> generator) {
            this.generator = generator;
        }

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
         * Builds a new session instance with the provided settings.
         * Assigns the session with {@link WorldDivider#associate(Session)}.
         *
         * @return The constructed session.
         */
        public Session complete() {
            Session session = new Session();

            WorldDivider.associate(session);

            if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
            if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;
            if (players != null) session.addPlayers(players);

            session.generator = generator.apply(session);

            if (players != null) {
                // todo move to generator?
                Arrays.asList(players).forEach(p -> {
                    p.session = session;
                    p.updateGeneratorSettings(session.generator);
                });
            }

            session.generator.island.build();

            return session;
        }
    }
}