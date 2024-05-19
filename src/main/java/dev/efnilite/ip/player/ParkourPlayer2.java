package dev.efnilite.ip.player;

import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.reward.Reward;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.world.Divider;
import dev.efnilite.ip.world.World;
import dev.efnilite.vilib.util.Strings;
import fr.mrmicky.fastboard.FastBoard;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParkourPlayer2 {

    private final Player player;
    private final PreviousData data;
    private final FastBoard board;

    private String boardTitle;
    private List<String> boardLines;

    public ParkourPlayer2(Player player) {
        this(player, new PreviousData(player));
    }

    public ParkourPlayer2(Player player, PreviousData data) {
        this.player = player;
        this.data = data;
        this.board = new FastBoard(player);
    }

    /**
     * Gets the player from the player list.
     * @param player The player to get.
     * @return The player.
     */
    public static ParkourPlayer2 as(Player player) {
        return Divider.sections.keySet().stream()
                .flatMap(section -> section.getPlayers().stream())
                .filter(p -> p.getPlayer() == player)
                .findFirst()
                .orElse(null);
    }

    public void join(Mode mode) {
        IP.log("Creating generator for %s with mode %s".formatted(getName(), mode.getName()));

        var generator = mode.getGenerator();

        var at = Divider.add(generator);

        generator.add(this);

        data.setup(at).thenRun(() -> generator.start(mode, at, mode.pointType));
    }

    /**
     * Leaves the current mode.
     * @param switchMode Whether the player is switching mode.
     * @param urgent Whether the player is leaving urgently, like on server shutdown.
     */
    public void leave(boolean switchMode, boolean urgent) {
        getGenerator().removePlayers(this);

        if (!switchMode && Config.CONFIG.getBoolean("proxy.enabled")) {
            sendToServer(Config.CONFIG.getString("proxy.return-server"));

            return;
        }

        data.reset(switchMode, urgent);

        if (switchMode) return;

        board.delete();
    }

    private void sendToServer(String server) {
        IP.log("Sending %s to proxy server %s".formatted(player.getName(), server));

        var out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(IP.getPlugin(), "BungeeCord", out.toByteArray());

            IP.log("Sent %s to proxy server %s".formatted(player.getName(), server));
        } catch (ChannelNotRegisteredException ex) {
            IP.logging().stack("%s is not registered with proxy".formatted(server), ex);
            player.kickPlayer("%s is not registered with proxy".formatted(server));
        }
    }

    /**
     * Updates the player's board.
     * @param score The score to display.
     * @param time The time to display.
     */
    public void updateBoard(double score, String time) {
        if (board == null || board.isDeleted() || !getGenerator().generator.profile.get("showScoreboard").asBoolean()) {
            return;
        }

        if (boardTitle.isEmpty()) {
            updateBoardValues();
        }

        board.updateTitle(boardTitle);
        board.updateLines(boardLines.stream().map(line -> updateLine(line, score, time)).toList());
    }

    // saves 6% performance!
    private void updateBoardValues() {
        boardTitle = Locales.getString(this, "scoreboard.title");
        boardLines = Locales.getStringList(this, "scoreboard.lines");
    }

    private String updateLine(String line, double score, String time) {
        var generator = getGenerator().generator;
        var leaderboard = generator.getMode().getLeaderboard();
        var top = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.getScoreAtRank(1);
        var high = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.get(getUUID());
        if (top == null) {
            top = new Score("?", "?", "?", 0);
        }

        var local = line.replace("%score%", Integer.toString(generator.score))
                .replace("%time%", generator.getFormattedTime())
                .replace("%difficulty%", Double.toString(generator.getDifficultyScore()))

                .replace("%top_score%", Integer.toString(top.score()))
                .replace("%top_player%", top.name())
                .replace("%top_time%", top.time())

                .replace("%high_score%", Integer.toString(high.score()))
                .replace("%high_score_time%", high.time());

        if (IP.getPlaceholderHook() != null) {
            return IP.getPlaceholderHook().replace(player, local);
        } else {
            return local;
        }
    }

    /**
     * Adds a reward to the player's settings.
     * @param mode The mode to add the reward to.
     * @param reward The reward to add.
     */
    public void addReward(Mode mode, Reward reward) {
        var set = data.leaveRewards.getOrDefault(mode, new HashSet<>());

        set.add(reward);

        data.leaveRewards.put(mode, set);
    }

    /**
     * Teleports the player.
     * @param vector The vector to teleport to.
     */
    public CompletableFuture<Boolean> teleport(Vector vector) {
        return PaperLib.teleportAsync(player, vector.toLocation(World.getWorld()));
    }

    /**
     * Teleports the player.
     * @param location The location to teleport to.
     */
    public CompletableFuture<Boolean> teleport(Location location) {
        return PaperLib.teleportAsync(player, location);
    }

    /**
     * Sends a message to the player.
     * @param message The message to send.
     */
    public void send(String message) {
        player.sendMessage(Strings.colour(message));
    }

    /**
     * Sends a path to the player.
     * @param path The path to send.
     */
    public void sendTranslated(String path, Object... args) {
        send(Locales.getString(player, path).formatted(args));
    }

    /**
     * Sends an action bar message to the player.
     * @param message The message to send.
     */
    public void sendActionBar(String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(Strings.colour(message)));
    }

    public Session getGenerator() {
        return Divider.sections.keySet().stream()
                .filter(p -> p.getPlayers().contains(this))
                .findFirst()
                .orElseThrow();
    }

    /**
     * @param permission The permission to check.
     * @return If the player has the permission.
     */
    public boolean hasPermission(String permission) {
        if (Config.CONFIG.getBoolean("permissions.enabled")) {
            return player.hasPermission(permission);
        }

        return true;
    }

    /**
     * @return If the player is a spectator.
     */
    public boolean isSpectator() {
        return player.getGameMode() == GameMode.SPECTATOR;
    }

    /**
     * @return The player's position as vector.
     */
    public Location getPosition() {
        return player.getLocation();
    }

    /**
     * @return The player's name.
     */
    public String getName() {
        return player.getName();
    }

    /**
     * @return The player's UUID.
     */
    public UUID getUUID() {
        return player.getUniqueId();
    }

    /**
     * @return The player's name.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The player's previous data.
     */
    public PreviousData getData() {
        return data;
    }

    /**
     * @return The player's scoreboard.
     */
    public FastBoard getBoard() {
        return board;
    }
}
