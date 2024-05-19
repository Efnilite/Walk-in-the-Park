package dev.efnilite.ip.player;

import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.reward.Reward;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParkourPlayer2 {

    private final Player player;
    private final PreviousData data;
    private final FastBoard board;

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

    /**
     * Leaves the current mode.
     * @param switchMode Whether the player is switching mode.
     * @param urgent Whether the player is leaving urgently, like on server shutdown.
     */
    public void leave(boolean switchMode, boolean urgent) {
        getGenerator().remove(this);

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
        player.sendMessage(message);
    }

    /**
     * Sends an action bar message to the player.
     * @param message The message to send.
     */
    public void sendActionBar(String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(Strings.colour(message)));
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
    public Vector getPosition() {
        return player.getLocation().toVector();
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
