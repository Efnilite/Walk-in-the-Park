package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.MultiGamemode;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.chat.ChatType;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.lib.fastboard.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author Efnilite
 */
public abstract class ParkourUser {

    /**
     * This user's locale
     */
    private String locale;

    /**
     * This user's scoreboard
     */
    public FastBoard board;

    /**
     * This user's session id
     */
    public String sessionId;

    /**
     * This user's PreviousData
     */
    public PreviousData previousData;

    /**
     * The selected {@link ChatType}
     */
    public ChatType chatType = ChatType.PUBLIC;

    /**
     * The Bukkit player instance associated with this user.
     */
    public final Player player;

    public static int JOIN_COUNT;

    protected static final Map<Player, ParkourUser> users = new HashMap<>();
    protected static final Map<Player, ParkourPlayer> players = new HashMap<>();

    public ParkourUser(@NotNull Player player, @Nullable PreviousData previousData) {
        this.player = player;
        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType()); // clear player effects
        }

        if ((boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD)) {
            this.board = new FastBoard(player);
        }
        // remove duplicates
        users.put(player, this);
    }

    /**
     * Registers a player. This registers the player internally.
     * This automatically unregisters the player if it is already registered.
     *
     * @param   player
     *          The player
     *
     * @return the ParkourPlayer instance of the newly joined player
     */
    public static @NotNull ParkourPlayer register(@NotNull Player player) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);
        if (existing != null) {
            data = existing.previousData;
            unregister(existing, false, false, true);
        }

        return ParkourPlayer.register0(new ParkourPlayer(player, data));
    }

    /**
     * This is the same as {@link #leave(ParkourUser)}, but instead for a Bukkit player instance.
     *
     * @see #leave(ParkourUser)
     *
     * @param   player
     *          The Bukkit player instance that will be removed from the game if the player is active.
     */
    public static void leave(@NotNull Player player) {
        ParkourUser user = getUser(player);
        if (user == null) {
            return;
        }
        leave(user);
    }

    /**
     * Makes a player leave. This sends a leave message to all other active Parkour players.
     * This uses {@link #unregister(ParkourUser, boolean, boolean, boolean)}, but with preset values.
     * Leaving always makes the player go back to their previous position, they will always be kicked if this plugin
     * is running Bungeecord mode, and their data will always be automatically saved. If you want to unregister a
     * player with different values for these params, please refer to using {@link #unregister(ParkourUser, boolean, boolean, boolean)}.
     *
     * @param   user
     *          The user instance
     */
    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, true);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param   user
     *          The user to unregister.
     *
     * @param   restorePreviousData
     *          Whether to restore the data from before the player joined the parkour.
     *
     * @param   kickIfBungee
     *          Whether to kick the player if Bungeecord mode is enabled.
     *
     * @param   saveAsync
     *          Whether to save player data asynchronously. This is recommended to be true
     *          at all times, unless your plugin is in the process of disabling.
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean saveAsync) {
        unregister0(user, restorePreviousData, kickIfBungee, saveAsync);
    }

    // Internal unregistering service
    @ApiStatus.Internal
    protected static void unregister0(@NotNull ParkourUser user, boolean sendBack, boolean kickIfBungee, boolean saveAsync) {
        Player pl = user.player;

        try {
            Session session = user.getSession();

            if (user instanceof ParkourPlayer pp) {
                ParkourGenerator generator = pp.getGenerator();

                int remaining = 0;
                // remove spectators
                if (session != null) {
                    if (session.getGamemode() instanceof MultiGamemode gamemode) {
                        gamemode.leave(pl, session);
                    }

                    session.removePlayers(pp);

                    if (session.getGamemode().getName().contains("team")) {
                        remaining = session.getPlayers().size();
                    }

                    for (ParkourSpectator spectator : session.getSpectators()) {
                        ParkourPlayer spp = ParkourPlayer.register(spectator.player);
                        IP.getDivider().generate(spp);

                        session.removeSpectators(spectator);
                    }
                }

                if (remaining == 0) {
                    // reset generator (remove blocks) and delete island
                    generator.reset(false);
                    IP.getDivider().leave(pp);
                }

                pp.save(saveAsync);
            } else if (user instanceof ParkourSpectator spectator) {
                spectator.unregister();

                if (session != null) {
                    spectator.getSession().removeSpectators(spectator);
                }
            }
            if (user.board != null && !user.board.isDeleted()) {
                user.board.delete();
            }
        } catch (Throwable throwable) { // safeguard to prevent people from losing data
            IP.logging().stack("Error while trying to make player " + user.getName() + " leave", throwable);
            user.send(IP.PREFIX + "<red>There was an error while trying to handle leaving.");
        }

        players.remove(pl);
        users.remove(pl);

        if (sendBack && Option.BUNGEECORD && kickIfBungee) {
            Util.sendPlayer(pl, IP.getConfiguration().getString("config", "bungeecord.return_server"));
            return;
        }
        if (user.previousData == null) {
            IP.logging().warn("No previous data found for " + user.getName());
        } else {
            user.previousData.apply(sendBack);

            if (user instanceof ParkourPlayer) {
                user.previousData.giveRewards((ParkourPlayer) user);
            }
        }

        pl.resetPlayerTime();
        pl.resetPlayerWeather();
    }

    /**
     * Gets a user from their UUID
     *
     * @param   uuid the user's UUID
     *
     * @return the ParkourUser instance associated with this uuid. Returns null if there isn't an active player.
     */
    public static @Nullable ParkourUser getUser(@NotNull UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return null;
        }

        return getUser(player);
    }

    /**
     * Gets a user from a Bukkit Player
     *
     * @param   player
     *          The Bukkit Player
     *
     * @return the associated {@link ParkourUser}
     */
    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        for (ParkourUser user : users.values()) {
            if (user.player.getUniqueId() == player.getUniqueId()) {
                return user;
            }
        }
        return null;
    }

    /**
     * Checks whether the provided player is a {@link ParkourUser}.
     * If the provided player is null, the method will automatically return false.
     *
     * @param   player
     *          The player. Can be null.
     *
     * @return True if the player is a registered {@link ParkourUser}.
     * False if the player isn't registered or the provided player is null.
     */
    @Contract("null -> false")
    public static boolean isUser(@Nullable Player player) {
        return player != null && users.containsKey(player);
    }

    /**
     * Checks whether the provided player is a {@link ParkourPlayer}.
     * If the provided player is null, the method will automatically return false.
     *
     * @param   player
     *          The player. Can be null.
     *
     * @return True if the player is a registered {@link ParkourPlayer}.
     * False if the player isn't registered or the provided player is null.
     */
    public static boolean isPlayer(@Nullable Player player) {
        return player != null && players.containsKey(player);
    }

    public static List<ParkourUser> getUsers() {
        return new ArrayList<>(users.values());
    }

    public static List<ParkourPlayer> getActivePlayers() {
        return new ArrayList<>(players.values());
    }

    /**
     * Teleports the player asynchronously, which helps with unloaded chunks (?)
     *
     * @param   to
     *          Where the player will be teleported to
     */
    public void teleport(@NotNull Location to) {
        player.leaveVehicle();
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Sends a message or array of it - coloured allowed, using the and sign
     *
     * @param   messages
     *          The message
     */
    public void send(String... messages) {
        for (String message : messages) {
            Util.send(player, message);
        }
    }

    /**
     * Sends a translated message
     *
     * @param   key
     *          The translation key
     *
     * @param   format
     *          Any objects that may be given to the formatting of the string.
     */
    public void sendTranslated(String key, Object... format) {
        send(Locales.getString(getLocale(), key, false).formatted(format));
    }

    /**
     * Updates the player's visual time.
     *
     * @param   selectedTime
     *          The selected time is the 24-hour format with 3 extra zeroes on the end.
     */
    public void updateVisualTime(int selectedTime) {
        int newTime = 18000 + selectedTime;
        if (newTime >= 24000) {
            newTime -= 24000;
        }

        player.setPlayerTime(newTime, false);
    }

    /**
     * Sets this player's locale
     *
     * @param   locale
     *          The locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Gets the UUID of the player
     *
     * @return the uuid
     */
    public UUID getUUID() {
        return player.getUniqueId();
    }

    /**
     * Gets the location of the player
     *
     * @return the player's location
     */
    public Location getLocation() {
        return player.getLocation();
    }

    /**
     * Gets this player's session by retrieving it from the session pool.
     *
     * @return the current player's {@link Session}
     */
    public Session getSession() {
        return Session.getSession(sessionId);
    }

    /**
     * Returns the player's locale
     *
     * @return the locale
     */
    public @NotNull String getLocale() {
        if (locale == null) {
            locale = (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
        }

        return locale;
    }

    /**
     * Returns the player's name
     *
     * @return the name of the player
     */
    public String getName() {
        return player.getName();
    }
}
