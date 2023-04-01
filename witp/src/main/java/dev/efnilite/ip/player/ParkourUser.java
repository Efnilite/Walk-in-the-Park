package dev.efnilite.ip.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.event.ParkourJoinEvent;
import dev.efnilite.ip.api.event.ParkourLeaveEvent;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.SessionChat;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.lib.fastboard.fastboard.FastBoard;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
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
    @NotNull
    public String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);

    /**
     * This user's scoreboard
     */
    public FastBoard board;

    /**
     * This user's PreviousData
     */
    @NotNull
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

        if (Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            this.board = new FastBoard(player);
        }
        users.put(player, this);
    }

    /**
     * Unregisters this user.
     */
    protected abstract void unregister();

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
            unregister(existing, false, false);
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
     * This uses {@link #unregister(ParkourUser, boolean, boolean)}, but with preset values.
     * Leaving always makes the player go back to their previous position, they will always be kicked if this plugin
     * is running Bungeecord mode, and their data will always be automatically saved. If you want to unregister a
     * player with different values for these params, please refer to using {@link #unregister(ParkourUser, boolean, boolean)}.
     *
     * @param user The user instance
     */
    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param user                The user to unregister.
     * @param restorePreviousData Whether to restore the data from before the player joined the parkour.
     * @param kickIfBungee        Whether to kick the player if Bungeecord mode is enabled.
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee) {
        new ParkourLeaveEvent(user).call();

        try {
            user.unregister();

            if (user.board != null && !user.board.isDeleted()) {
                user.board.delete();
            }
        } catch (Exception ex) { // safeguard to prevent people from losing data
            IP.logging().stack("Error while trying to make player %s leave".formatted(user.getName()), ex);
            user.send("<red><bold>There was an error while trying to handle leaving.");
        }

        players.remove(user.player);
        users.remove(user.player);

        if (restorePreviousData && Option.ON_JOIN && kickIfBungee) {
            sendPlayer(user.player, Config.CONFIG.getString("bungeecord.return_server"));
            return;
        }

        user.previousData.apply(restorePreviousData);

        if (user instanceof ParkourPlayer player) {
            user.previousData.onLeave.forEach(r -> r.execute(player));
        }

        user.player.resetPlayerTime();
        user.player.resetPlayerWeather();
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
        List<ParkourUser> filtered = getUsers().stream().filter(other -> other.getUUID() == player.getUniqueId()).toList();

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
        send(Locales.getString(locale, key).formatted(format));
    }

    /**
     * Updates the scoreboard for the specified generator.
     * @param generator The generator.
     */
    public void updateScoreboard(ParkourGenerator generator) {
        // board can be null a few ticks after on player leave
        if (board == null || board.isDeleted() || !generator.profile.get("showScoreboard").asBoolean()) {
            return;
        }

        Leaderboard leaderboard = generator.getMode().getLeaderboard();

        String title = Strings.colour(Util.translate(player, Locales.getString(locale, "scoreboard.title")));
        List<String> lines = new ArrayList<>();

        Score top = new Score("?", "?", "?", 0);
        if (leaderboard != null && leaderboard.getScoreAtRank(1) != null) {
            top = leaderboard.getScoreAtRank(1);
        }

        // update lines
        for (String line : Locales.getStringList(locale, "scoreboard.lines").stream().map(Strings::colour).toList()) {
            lines.add(replace(Util.translate(player, line), top, generator));
        }

        board.updateTitle(replace(title, top, generator));
        board.updateLines(lines);
    }

    private String replace(String s, Score top, ParkourGenerator generator) {
        return s.replace("%score%", Integer.toString(generator.score))
                .replace("%time%", generator.getTime())
                .replace("%highscore%", Integer.toString(top.score()))
                .replace("%highscoretime%", top.time())
                .replace("%topscore%", Integer.toString(top.score()))
                .replace("%topplayer%", top.name())
                .replace("%session%", session.getPlayers().get(0).getName());
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
     * @return The player's name
     */
    public String getName() {
        return player.getName();
    }
}