package dev.efnilite.ip.session;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.world.Divider;
import org.bukkit.Location;
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
     * The spawn location of this session.
     */
    private Location spawnLocation;

    /**
     * The generator.
     */
    public ParkourGenerator generator;

    /**
     * The visibility of this session. Default public.
     */
    private Visibility visibility = Visibility.PUBLIC;

    /**
     * List of muted users.
     */
    private final List<ParkourUser> muted = new ArrayList<>();

    /**
     * List of users.
     */
    private final Map<UUID, ParkourUser> users = new HashMap<>();

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
        IP.log("Creating session");

        if (players != null) {
            IP.log("Players in session: %s".formatted(Arrays.stream(players).map(Player::getName).toList()));
        }

        Session session = new Session();

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

        session.spawnLocation = Divider.add(session);
        session.generator = generatorFunction.apply(session);

        if (players != null) {
            pps.forEach(p -> p.updateGeneratorSettings(session.generator));
        }

        session.generator.island.build(session.spawnLocation);

        return session;
    }

    /**
     * Sets the visibility of this session.
     * @param visibility The visibility.
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * @return The visibility of this session.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Adds provided players to this session's player list.
     *
     * @param toAdd The players to add.
     */
    public void addPlayers(ParkourPlayer... toAdd) {
        for (ParkourPlayer player : toAdd) {
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
     * @return the spawn location for this {@link Session}.
     */
    @SuppressWarnings("unused")
    public Location getSpawnLocation() {
        return spawnLocation.clone();
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
     * @param user The user.
     * @return True when the user is muted, false if not.
     */
    public boolean isMuted(@NotNull ParkourUser user) {
        return muted.contains(user);
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