package dev.efnilite.ip.session;

import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A session is bound to a {@link dev.efnilite.ip.world.WorldDivider} section.
 * It manages all players, all spectators, visibility, the generator, etc.
 * <p>
 * Iteration 2.
 */
public class Session2 {

    /**
     * The generator.
     */
    public ParkourGenerator generator;

    /**
     * The visibility of this session.
     */
    public Visibility visibility = Visibility.PUBLIC;

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
     * Returns whether this session is accepting players.
     *
     * @return True if yes, false if no.
     */
    public boolean isAcceptingPlayers() {
        return false;
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
     * Returns whether this session is accepting spectators.
     *
     * @return True if yes, false if no.
     */
    public boolean isAcceptingSpectators() {
        return visibility == Visibility.PUBLIC;
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
}