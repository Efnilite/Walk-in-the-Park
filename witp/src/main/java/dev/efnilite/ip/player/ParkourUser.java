package dev.efnilite.ip.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.api.MultiGamemode;
import dev.efnilite.ip.api.event.ParkourJoinEvent;
import dev.efnilite.ip.api.event.ParkourLeaveEvent;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.SessionChat;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.vilib.lib.fastboard.fastboard.FastBoard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author Efnilite
 */
public abstract class ParkourUser {

    /**
     * This player's session.
     */
    public Session session;

    /**
     * This user's locale
     */
    private String locale;

    /**
     * This user's scoreboard
     */
    public FastBoard board;

    /**
     * This user's PreviousData
     */
    public PreviousData previousData;

    /**
     * The selected {@link SessionChat.ChatType}
     */
    public SessionChat.ChatType chatType = SessionChat.ChatType.PUBLIC;

    /**
     * The Bukkit player instance associated with this user.
     */
    public final Player player;

    /**
     * The {@link Instant} when the player joined.
     */
    public final Instant joined;

    public static int JOIN_COUNT;

    private static final Map<Player, ParkourUser> users = new HashMap<>();
    private static final Map<Player, ParkourPlayer> players = new HashMap<>();

    public ParkourUser(@NotNull Player player, @Nullable PreviousData previousData) {
        this.player = player;
        this.joined = Instant.now();

        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType()); // clear player effects
        }

        if (Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            this.board = new FastBoard(player);
        }
        // remove duplicates
        users.put(player, this);
    }

    /**
     * Registers a player. This registers the player internally.
     * This automatically unregisters the player if it is already registered.
     *
     * @param player The player
     * @return the ParkourPlayer instance of the newly joined player
     */
    public static @NotNull ParkourPlayer register(@NotNull Player player) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);
        if (existing != null) {
            data = existing.previousData;
            unregister(existing, false, false, true);
        }

        ParkourPlayer pp = new ParkourPlayer(player, data);

        // stats
        JOIN_COUNT++;
        new ParkourJoinEvent(pp).call();

        IP.getStorage().readPlayer(pp);
        players.put(pp.player, pp);
        return pp;
    }

    /**
     * This is the same as {@link #leave(ParkourUser)}, but instead for a Bukkit player instance.
     *
     * @param player The Bukkit player instance that will be removed from the game if the player is active.
     * @see #leave(ParkourUser)
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
     * @param user The user instance
     */
    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, true);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param user                The user to unregister.
     * @param restorePreviousData Whether to restore the data from before the player joined the parkour.
     * @param kickIfBungee        Whether to kick the player if Bungeecord mode is enabled.
     * @param saveAsync           Whether to save player data asynchronously. This is recommended to be true
     *                            at all times, unless your plugin is in the process of disabling.
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean saveAsync) {
        Player pl = user.player;

        new ParkourLeaveEvent(user).call();

        try {
            Session session = user.session;

            if (user instanceof ParkourPlayer pp) {
                ParkourGenerator generator = pp.generator;

                int remaining = 0;
                // remove spectators
                if (session != null) {
                    if (generator.getGamemode() instanceof MultiGamemode gamemode) {
                        gamemode.leave(pl, session);
                    }

                    session.removePlayers(pp);

                    if (generator.getGamemode().getName().contains("team")) {
                        remaining = session.getPlayers().size();
                    }

                    for (ParkourSpectator spectator : session.getSpectators()) {
                        Gamemodes.DEFAULT.create(spectator.player);

                        session.removeSpectators(spectator);
                    }
                }

                if (remaining == 0) {
                    // reset generator (remove blocks) and delete island
                    generator.reset(false);

                    WorldDivider.disassociate(session);
                }

                pp.save(saveAsync);
            } else if (user instanceof ParkourSpectator spectator) {
                spectator.unregister();
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

        if (restorePreviousData && Option.BUNGEECORD && kickIfBungee) {
            sendPlayer(pl, Config.CONFIG.getString("bungeecord.return_server"));
            return;
        }
        if (user.previousData == null) {
            IP.logging().warn("No previous data found for " + user.getName());
        } else {
            user.previousData.apply(restorePreviousData);

            if (user instanceof ParkourPlayer) {
                user.previousData.giveRewards((ParkourPlayer) user);
            }
        }

        pl.resetPlayerTime();
        pl.resetPlayerWeather();
    }

    // Sends a player to a BungeeCord server. server is the server name.
    private static void sendPlayer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        try {
            player.sendPluginMessage(IP.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            IP.logging().error("Tried to send " + player.getName() + " to server " + server + " but this server is not registered!");
            player.kickPlayer("There was an error while trying to move you to server " + server + ", please rejoin.");
        }
    }

    /**
     * Gets a user from a Bukkit Player
     *
     * @param player The Bukkit Player
     * @return the associated {@link ParkourUser}
     */
    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        List<ParkourUser> filtered = Colls.filter(other -> other.getUUID() == player.getUniqueId(), getUsers());

        return filtered.size() > 0 ? filtered.get(0) : null;
    }

    /**
     * Checks whether the provided player is a {@link ParkourUser}.
     * If the provided player is null, the method will automatically return false.
     *
     * @param player The player. Can be null.
     * @return True if the player is a registered {@link ParkourUser}.
     * False if the player isn't registered or the provided player is null.
     */
    public static boolean isUser(@Nullable Player player) {
        return player != null && users.containsKey(player);
    }

    /**
     * Checks whether the provided player is a {@link ParkourPlayer}.
     * If the provided player is null, the method will automatically return false.
     *
     * @param player The player. Can be null.
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
     * @param to Where the player will be teleported to
     */
    public void teleport(@NotNull Location to) {
        player.leaveVehicle();
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Sends a message or array of it - coloured allowed, using the and sign
     *
     * @param messages The message
     */
    public void send(String... messages) {
        for (String message : messages) {
            Util.send(player, message);
        }
    }

    /**
     * Sends a translated message
     *
     * @param key    The translation key
     * @param format Any objects that may be given to the formatting of the string.
     */
    public void sendTranslated(String key, Object... format) {
        send(Locales.getString(getLocale(), key).formatted(format));
    }

    /**
     * Updates the player's visual time.
     *
     * @param selectedTime The selected time is the 24-hour format with 3 extra zeroes on the end.
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
     * @param locale The locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return The player's uuid
     */
    public UUID getUUID() {
        return player.getUniqueId();
    }

    /**
     * @return The player's location
     */
    public Location getLocation() {
        return player.getLocation();
    }

    /**
     * @return The player's locale
     */
    public @NotNull String getLocale() {
        if (locale == null) {
            locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
        }

        return locale;
    }

    /**
     * @return The player's name
     */
    public String getName() {
        return player.getName();
    }
}
