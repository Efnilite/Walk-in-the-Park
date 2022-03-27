package dev.efnilite.witp.player;

import dev.efnilite.fycore.chat.Message;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.PlayerLeaveEvent;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.session.Session;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.SelectStatement;
import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author Efnilite
 */
public abstract class ParkourUser {

    public static int JOIN_COUNT;

    /**
     * This user's session id
     */
    protected String sessionId;

    /**
     * This user's locale
     */
    protected String locale;

    /**
     * This user's scoreboard
     */
    protected FastBoard board;

    /**
     * This user's PreviousData
     */
    protected PreviousData previousData;

    /**
     * The Bukkit player instance associated with this user.
     */
    protected final Player player;

    public static Map<UUID, Integer> highScores = new LinkedHashMap<>();
    protected static volatile Map<UUID, Highscore> scoreMap = new LinkedHashMap<>();
    protected static final Map<UUID, ParkourUser> users = new HashMap<>();
    protected static final Map<Player, ParkourPlayer> players = new HashMap<>();

    public ParkourUser(@NotNull Player player, @Nullable PreviousData previousData) {
        this.player = player;
        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType()); // clear player effects
        }

        if (Option.SCOREBOARD.get()) {
            this.board = new FastBoard(player);
        }
        // remove duplicates
        users.put(player.getUniqueId(), this);
    }

    /**
     * Updates the scoreboard
     */
    public abstract void updateScoreboard();

    /**
     * Joins a player. This sends a join message while using {@link #register(Player)}.
     *
     * @param   player
     *          The player
     *
     * @return the newly registered ParkourPlayer instance.
     */
    public static @NotNull ParkourPlayer join(@NotNull Player player) {
        ParkourPlayer pp = register(player);
        WITP.getDivider().generate(pp);

        if (Option.JOIN_LEAVE_MESSAGES.get()) {
            pp.sendTranslated("join", player.getName());
            for (ParkourUser to : getUsers()) {
                if (to.getUUID().equals(player.getUniqueId())) {
                    continue;
                }

                to.sendTranslated("player-join", player.getName());
            }
        }

        return pp;
    }

    /**
     * Registers a player. This may be used to internally register a player without a joining message.
     * Doesn't use async reading because the system immediately needs the data.
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
            data = existing.getPreviousData();
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
        if (Option.JOIN_LEAVE_MESSAGES.get()) {
            user.sendTranslated("leave", user.getPlayer().getName());
            for (ParkourUser to : getUsers()) {
                if (to.getUUID().equals(user.getUUID())) {
                    continue;
                }

                to.sendTranslated("player-leave", user.player.getName());
            }
        }

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
        Player pl = user.getPlayer();

        try {
            new PlayerLeaveEvent(user).call();
            Session session = user.getSession();

            if (user instanceof ParkourPlayer) {
                ParkourPlayer pp = (ParkourPlayer) user;

                ParkourGenerator generator = pp.getGenerator();
                // remove spectators
                if (session != null) {
                    for (ParkourSpectator spectator : session.getSpectators()) {
                        ParkourPlayer spp = ParkourPlayer.register(spectator.getPlayer());
                        WITP.getDivider().generate(spp);

                        session.removeSpectators(spectator);
                    }
                }

                // reset generator (remove blocks) and delete island
                generator.reset(false);
                WITP.getDivider().leave(pp);
                pp.save(saveAsync);
            } else if (user instanceof ParkourSpectator) {
                ParkourSpectator spectator = (ParkourSpectator) user;
                spectator.stopClosestChecker();
                if (session != null) {
                    spectator.getSession().removeSpectators(spectator);
                }
            }
            if (user.getBoard() != null && !user.getBoard().isDeleted()) {
                user.getBoard().delete();
            }
        } catch (Throwable throwable) { // safeguard to prevent people from losing data
            Logging.stack("Error while trying to make player " + user.getPlayer().getName() + " leave",
                    "Please report this error to the developer. Previous data will still be set.", throwable);
            user.send(WITP.PREFIX + "<red>There was an error while trying to handle leaving.");
        }

        players.remove(pl);
        users.remove(pl.getUniqueId());

        if (sendBack && Option.BUNGEECORD.get() && kickIfBungee) {
            Util.sendPlayer(pl, WITP.getConfiguration().getString("config", "bungeecord.return_server"));
            return;
        }
        if (user.getPreviousData() == null) {
            Logging.warn("No previous data found for " + user.getPlayer().getName());
        } else {
            user.getPreviousData().apply(sendBack);

            if (user instanceof ParkourPlayer) {
                user.getPreviousData().giveRewards((ParkourPlayer) user);
            }
        }
        pl.resetPlayerTime();
        pl.resetPlayerWeather();
    }

    /**
     * Gets the highscores of all player
     *
     * @throws  IOException
     *          When creating the file reader goes wrong
     */
    public static void fetchHighScores() throws IOException, SQLException {
        if (Option.SQL.get()) {
            SelectStatement per = new SelectStatement(WITP.getSqlManager(), Option.SQL_PREFIX.get() + "players")
                    .addColumns("uuid", "name", "highscore", "hstime", "hsdiff");
            HashMap<String, List<Object>> stats = per.fetch();
            if (stats != null && stats.size() > 0) {
                for (String string : stats.keySet()) {
                    List<Object> values = stats.get(string);
                    UUID uuid = UUID.fromString(string);
                    String name = (String) values.get(0);
                    int highScore = Integer.parseInt((String) values.get(1));
                    String highScoreTime = (String) values.get(2);
                    String highScoreDiff = (String) values.get(3);
                    highScores.put(uuid, highScore);
                    scoreMap.put(uuid, new Highscore(name, highScoreTime, highScoreDiff));
                }
            }
        } else {
            File folder = new File(WITP.getInstance().getDataFolder() + "/players/");
            if (!(folder.exists())) {
                folder.mkdirs();
                return;
            }
            for (File file : folder.listFiles()) {
                FileReader reader = new FileReader(file);
                ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);
                if (from == null) {
                    continue;
                }
                String name = file.getName();
                UUID uuid = UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                if (from.highScoreDifficulty == null) {
                    from.highScoreDifficulty = "?";
                }
                highScores.put(uuid, from.highScore);
                scoreMap.put(uuid, new Highscore(from.name, from.highScoreTime, from.highScoreDifficulty));
                reader.close();
            }
        }
    }

    /**
     * Resets all known highscores.
     *
     * @return true if success. false if the resetting failed.
     */
    public static boolean resetHighScores() {
        for (ParkourPlayer player : ParkourPlayer.getActivePlayers()) { // active players
            player.setHighScore(player.name, 0, "0.0s", "0.0");
        }

        File folder = new File(WITP.getInstance().getDataFolder(), "players/"); // update files

        try {
            for (File file : folder.listFiles()) {
                FileReader reader = new FileReader(file);
                ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);
                from.uuid = UUID.fromString(file.getName().replace(".json", ""));
                from.setHighScore(from.name, 0, "0.0s", "0.0");
                from.save(true);
                reader.close();
            }
        } catch (Throwable throwable) {
            Logging.stack("Error while trying to reset the high scores!",
                    "Please try again or report this error to the developer!", throwable);
            return false;
        }
        return true;
    }

    /**
     * Resets a specific player's highscore
     *
     * @param   uuid
     *          The uuid
     *
     * @return true if success. false if resetting failed.
     */
    public static boolean resetHighscore(UUID uuid) {
        ParkourPlayer player = ParkourPlayer.getPlayer(uuid);
        if (player != null) {
            player.setHighScore(player.name, 0, "0.0s", "0.0");
        } else {
            File file = new File(WITP.getInstance().getDataFolder(), "players/" + uuid.toString() + ".json");

            try {
                FileReader reader = new FileReader(file);
                ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);
                from.uuid = UUID.fromString(file.getName().replace(".json", ""));
                from.setHighScore(from.name, 0, "0.0s", "0.0");
                from.save(true);
                reader.close();
            } catch (Throwable throwable) {
                Logging.stack("Error while trying to reset the high scores!",
                        "Please try again or report this error to the developer!", throwable);
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes the high scores
     */
    public static void initHighScores() {
        if (highScores.isEmpty()) {
            try {
                fetchHighScores();
            } catch (IOException | SQLException ex) {
                Logging.stack("Error while trying to fetch the high scores!",
                        "Please try again or report this error to the developer!", ex);
            }
            highScores = Util.sortByValue(highScores);
        }
    }

    /**
     * Gets an instance of a {@link Highscore} with the player's uuid
     *
     * @param   uuid
     *          The player's uuid
     *
     * @return the Highscore instance associated with this player uuid
     */
    public static Highscore getHighscore(@NotNull UUID uuid) {
        return scoreMap.get(uuid);
    }

    /**
     * Gets the highest score of a player from their uuid
     *
     * @param   uuid
     *          The player's uuid
     *
     * @return the highest score they got
     */
    public static int getHighestScore(@NotNull UUID uuid) {
        return highScores.get(uuid);
    }

    /**
     * Gets the rank of a certain player
     *
     * @param   player
     *          The player
     *
     * @return the rank (starts at 1.)
     */
    public static int getRank(UUID player) {
        return new ArrayList<>(highScores.keySet()).indexOf(player) + 1;
    }

    /**
     * Gets a user from their UUID
     *
     * @param   uuid the user's UUID
     *
     * @return the ParkourUser instance associated with this uuid. Returns null if there isn't an active player.
     */
    public static @Nullable ParkourUser getUser(@NotNull UUID uuid) {
        for (ParkourUser user : users.values()) {
            if (user.player.getUniqueId() == uuid) {
                return user;
            }
        }
        return null;
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
        return getUser(player.getUniqueId());
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
        if (to.getWorld() != null) {
            to.getWorld().getChunkAt(to);
        }
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Sends a message or array of it - coloured allowed, using the and sign
     *
     * @param   messages
     *          The message
     */
    public void send(String... messages) {
        for (String msg : messages) {
            Message.send(player, msg);
        }
    }

    /**
     * Gets a message from messages-v3.yml
     *
     * @param   path
     *          The path name in messages-v3.yml (for example: 'time-preference')
     *
     * @param   replaceable
     *          What can be replaced (for example: %s to yes)
     */
    public void sendTranslated(String path, String... replaceable) {
        String message = getTranslated(path, replaceable);
        if (WITP.getPlaceholderHook() == null) {
            send(message);
        } else {
            send(PlaceholderAPI.setPlaceholders(player, message));
        }
    }

    /**
     * Same as {@link #sendTranslated(String, String...)}, but without sending the text (used in GUIs)
     *
     * @param   path
     *          The path
     *
     * @param   replaceable
     *          Things that can be replaced
     *
     * @return the coloured and replaced string
     */
    public String getTranslated(String path, String... replaceable) {
        path = "messages." + getLocale() + "." + path;
        String message = WITP.getConfiguration().getLang("lang", path);

        for (String s : replaceable) {
            message = message.replaceFirst("%[a-z]", s);
        }

        return message;
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
     * Sets the player's session id
     *
     * @param   sessionId
     *          The session id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the scoreboard of the player
     *
     * @return the {@link FastBoard} of the player
     */
    public @Nullable FastBoard getBoard() {
        return board;
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
     * Gets the previous data of the player
     *
     * @return the {@link PreviousData} of the player
     */
    public PreviousData getPreviousData() {
        return previousData;
    }

    /**
     * Gets the Bukkit version of the player
     *
     * @return the player
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Returns the player's locale
     *
     * @return the locale
     */
    public @NotNull String getLocale() {
        if (locale == null) {
            locale = Option.DEFAULT_LANG.get();
        }

        return locale;
    }
}
