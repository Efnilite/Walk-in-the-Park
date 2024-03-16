package dev.efnilite.ip.session;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
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
    public final List<ParkourUser> muted = new ArrayList<>();

    /**
     * List of users.
     */
    protected final Map<UUID, ParkourUser> users = new HashMap<>();

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
        Session session = new Session();

        Divider.add(session);

        if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
        if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;

        List<ParkourPlayer> pps = new ArrayList<>();
        if (players != null) {
            for (Player player : players) {
                ParkourPlayer pp = ParkourUser.register(player, session);
                session.addPlayers(pp);
                pps.add(pp);
            }
        }

        session.generator = generatorFunction.apply(session);

        if (players != null) {
            pps.forEach(p -> p.updateGeneratorSettings(session.generator));
        }

        session.generator.island.build();

        return session;
    }

    /**
     * Adds provided players to this session's player list.
     *
     * @param toRemove The players to add.
     */
    public void addPlayers(ParkourPlayer... toRemove) {
        for (ParkourPlayer player : toRemove) {
            IP.log("Adding player %s to session".formatted(player.getName()));

            for (ParkourPlayer to : getPlayers()) {
                to.send(Locales.getString(player.locale, "lobby.other_join").formatted(player.getName()));
            }

            users.put(player.getUUID(), player);
        }
    }

    /**
     * Removes provided players from this session's player list.
     *
     * @param toRemove The players to remove.
     */
    public void removePlayers(ParkourPlayer... toRemove) {
        for (ParkourPlayer player : toRemove) {
            IP.log("Removing player %s from session".formatted(player.getName()));

            users.remove(player.getUUID());
        }

        List<ParkourPlayer> players = getPlayers();
        for (ParkourPlayer player : toRemove) {
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
    public List<ParkourPlayer> getPlayers() {
        return users.values().stream()
                .filter(user -> user instanceof ParkourPlayer)
                .map(user -> (ParkourPlayer) user)
                .toList();
    }

    /**
     * Adds provided spectators to this session's spectator list.
     *
     * @param spectators The spectators to add.
     */
    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            IP.log("Adding spectator %s to session".formatted(spectator.getName()));

            for (ParkourPlayer player : getPlayers()) {
                player.sendTranslated("play.spectator.other_join", spectator.getName());
            }

            users.put(spectator.getUUID(), spectator);
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

            for (ParkourPlayer player : getPlayers()) {
                player.sendTranslated("play.spectator.other_leave", spectator.getName());
            }

            users.remove(spectator.getUUID());
        }
    }

    /**
     * @return The spectators.
     */
    public List<ParkourSpectator> getSpectators() {
        return users.values().stream()
                .filter(user -> user instanceof ParkourSpectator)
                .map(user -> (ParkourSpectator) user)
                .toList();
    }

    /**
     * @return The users.
     */
    public List<ParkourUser> getUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void toggleMute(@NotNull ParkourUser user) {
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