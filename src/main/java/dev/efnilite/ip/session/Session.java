package dev.efnilite.ip.session;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.world.Divider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * <p>A session is bound to a {@link Divider} section.
 * It manages all players, all spectators, visibility, the generator, etc.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Session {

    /**
     * List of muted users.
     */
    public final List<ParkourPlayer2> muted = new ArrayList<>();

    /**
     * List of users.
     */
    protected final Map<UUID, ParkourPlayer2> players = new HashMap<>();

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
     * Creates a new session.
     *
     * @param generatorFunction     The generator function.
     * @param isAcceptingPlayers    The function that takes the current session and returns whether new players should be accepted.
     * @param isAcceptingSpectators The function that takes the current session and returns whether new spectators should be accepted.
     * @param players               The players.
     * @return The session.
     */
    public static Session create(Function<Session, ParkourGenerator> generatorFunction,
                                 Function<Session, Boolean> isAcceptingPlayers,
                                 Function<Session, Boolean> isAcceptingSpectators,
                                 Player... players) {
        IP.log("Creating session with players %s".formatted(Arrays.toString(players)));

        Session session = new Session();

        var location = Divider.add(session);

        if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
        if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;

        List<ParkourPlayer2> pps = new ArrayList<>();
        if (players != null) {
            for (Player player : players) {
                ParkourPlayer pp = new ParkourPlayer2(player, session);
                session.addPlayers(pp);
                pps.add(pp);
            }
        }

        session.generator = generatorFunction.apply(session);

        if (players != null) {
            pps.forEach(p -> p.updateGeneratorSettings(session.generator));
        }

        session.generator.island.build(location);

        return session;
    }

    /**
     * Adds provided players to this session's player list.
     *
     * @param toAdd The players to add.
     */
    public void addPlayers(ParkourPlayer2... toAdd) {
        for (ParkourPlayer2 player : toAdd) {
            IP.log("Adding player %s to session".formatted(player.getName()));

            for (ParkourPlayer to : getPlayers()) {
                to.send(Locales.getString(player.locale, "lobby.other_join").formatted(player.getName()));
            }

            players.put(player.getUUID(), player);
        }
    }

    /**
     * Removes provided players from this session's player list.
     *
     * @param toRemove The players to remove.
     */
    public void removePlayers(ParkourPlayer2... toRemove) {
        for (ParkourPlayer2 player : toRemove) {
            IP.log("Removing player %s from session".formatted(player.getName()));

            players.remove(player.getUUID());
        }

        List<ParkourPlayer2> players = getPlayers();
        for (ParkourPlayer2 player : toRemove) {
            for (ParkourPlayer to : players) {
                to.send(Locales.getString(player.locale, "lobby.other_leave").formatted(player.getName()));
            }
        }

        if (toRemove.length > 0 && players.isEmpty()) {
            generator.reset(false);
            Divider.remove(this);
        }
    }

    /**
     * @return The players.
     */
    public Collection<ParkourPlayer2> getPlayers() {
        return players.values();
    }

    /**
     * Adds provided spectators to this session's spectator list.
     *
     * @param spectators The spectators to add.
     */
    public void addSpectators(ParkourPlayer2... spectators) {
        for (ParkourPlayer2 spectator : spectators) {
            IP.log("Adding spectator %s to session".formatted(spectator.getName()));

            for (ParkourPlayer2 player : getPlayers()) {
                player.sendTranslated("play.spectator.other_join", spectator.getName());
            }

            players.put(spectator.getUUID(), spectator);
        }
    }

    /**
     * Removes provided spectators from this session's spectator list.
     *
     * @param spectators The spectators to remove.
     */
    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            IP.log("Removing spectator %s from session".formatted(spectator.getName()));

            for (ParkourPlayer2 player : getPlayers()) {
                player.sendTranslated("play.spectator.other_leave", spectator.getName());
            }

            players.remove(spectator.getUUID());
        }
    }

    /**
     * @return The spectators.
     */
    public List<ParkourPlayer2> getSpectators() {
        return players.values().stream()
                .filter(ParkourPlayer2::isSpectator)
                .toList();
    }

    /**
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void toggleMute(@NotNull ParkourPlayer2 user) {
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
     * An enum for all available chat types that a player can select while playing
     */
    public enum ChatType {

        LOBBY_ONLY, PLAYERS_ONLY, PUBLIC

    }
}