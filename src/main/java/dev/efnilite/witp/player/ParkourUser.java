package dev.efnilite.witp.player;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.PlayerLeaveEvent;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.sql.SelectStatement;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Class to envelop every user in WITP.
 */
public abstract class ParkourUser {

    public String locale;
    protected FastBoard board;
    protected PreviousData previousData;
    protected final Player player;

    public static int JOIN_COUNT;

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
     * Unregisters a ParkourPlayer
     *
     * @param   player
     *          The ParkourPlayer
     *
     * @throws  IOException
     *          When saving the player's file goes wrong
     */
    public static void unregister(@NotNull ParkourUser player, boolean sendBack, boolean kickIfBungee, boolean saveAsync)
            throws IOException, InvalidStatementException {
        Player pl = player.getPlayer();

        try {
            new PlayerLeaveEvent(player).call();
            if (player instanceof ParkourPlayer) {
                ParkourPlayer pp = (ParkourPlayer) player;

                ParkourGenerator generator = pp.getGenerator();
                // remove spectators
                for (ParkourSpectator spectator : generator.getSpectators()) {
                    ParkourPlayer spp = ParkourPlayer.register(spectator.getPlayer(), spectator.previousData);
                    WITP.getDivider().generate(spp);

                    generator.removeSpectators(spectator);
                }

                // reset generator (remove blocks) and delete island
                generator.reset(false);
                WITP.getDivider().leave(pp);
                pp.save(saveAsync);
            } else if (player instanceof ParkourSpectator) {
                ParkourSpectator spectator = (ParkourSpectator) player;
                spectator.watching.removeSpectators(spectator);
            }
            if (player.getBoard() != null && !player.getBoard().isDeleted()) {
                player.getBoard().delete();
            }
        } catch (Throwable throwable) { // safeguard to prevent people from losing data
            Logging.stack("Error while trying to make player " + player.getPlayer().getName() + " leave",
                    "Please report this error to the developer. Inventory will still be set", throwable);
            player.send("&4&l> &cThere was an error while trying to handle leaving.");
        }

        players.remove(pl);
        users.remove(pl.getUniqueId());

        if (sendBack && Option.BUNGEECORD.get() && kickIfBungee) {
            Util.sendPlayer(pl, WITP.getConfiguration().getString("config", "bungeecord.return_server"));
            return;
        }
        if (player.getPreviousData() == null) {
            Logging.warn("No previous data found for " + player.getPlayer().getName());
            return;
        } else {
            player.getPreviousData().apply(sendBack);
        }
        pl.resetPlayerTime();
        pl.resetPlayerWeather();

        if (Option.REWARDS.get() && Option.LEAVE_REWARDS.get() && player instanceof ParkourPlayer) {
            ParkourPlayer pp = (ParkourPlayer) player;
            if (pp.getGenerator() instanceof DefaultGenerator) {
                for (String command : ((DefaultGenerator) pp.getGenerator()).getLeaveRewards()) {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            command.replace("%player%", player.getPlayer().getName()));
                }
            }
        }
    }

    public boolean alertCheckPermission(String perm) {
        if (Option.PERMISSIONS.get()) {
            boolean check = player.hasPermission(perm);
            if (!check) {
                sendTranslated("cant-do");
            }
            return check;
        }
        return true;
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
     * Gets the highscores of all player
     *
     * @throws  IOException
     *          When creating the file reader goes wrong
     */
    public static void fetchHighScores() throws IOException, SQLException {
        if (Option.SQL.get()) {
            SelectStatement per = new SelectStatement(WITP.getDatabase(), Option.SQL_PREFIX.get() + "players")
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

    public static void resetHighScores() throws IOException {
        for (ParkourPlayer player : ParkourPlayer.getActivePlayers()) { // active players
            player.setHighScore(player.name, 0, "0.0s", "0.0");
        }

        File folder = new File(WITP.getInstance().getDataFolder() + "/players/"); // update files
        if (!(folder.exists())) {
            folder.mkdirs();
            return;
        }
        for (File file : folder.listFiles()) {
            FileReader reader = new FileReader(file);
            ParkourPlayer from = WITP.getGson().fromJson(reader, ParkourPlayer.class);
            from.uuid = UUID.fromString(file.getName().replace(".json", ""));
            from.setHighScore(from.name, 0, "0.0s", "0.0");
            from.save(true);
            reader.close();
        }
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
     * Sends a message or array of it - coloured allowed, using the and sign
     *
     * @param   messages
     *          The message
     */
    public void send(String... messages) {
        for (String msg : messages) {
            player.sendMessage(Util.color(msg));
        }
    }

    public static List<ParkourUser> getUsers() {
        return new ArrayList<>(users.values());
    }

    public static List<ParkourPlayer> getActivePlayers() {
        return new ArrayList<>(players.values());
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
     * Gets a message from messages-v3.yml
     *
     * @param   path
     *          The path name in messages-v3.yml (for example: 'time-preference')
     *
     * @param   replaceable
     *          What can be replaced (for example: %s to yes)
     */
    public void sendTranslated(String path, String... replaceable) {
        path = "messages." + this.locale + "." + path;
        send(replace(WITP.getConfiguration().getString("lang", path), replaceable));
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
        path = "messages." + locale + "." + path;
        return replace(WITP.getConfiguration().getString("lang", path), replaceable);
    }

    // Replaces %s, etc. with replaceable arguments
    private String replace(String string, String... replaceable) {
        for (String s : replaceable) {
            string = string.replaceFirst("%[a-z]", s);
        }
        return string;
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
     * Gets the previous data of the player
     *
     * @return the previousdata of the player
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
}
