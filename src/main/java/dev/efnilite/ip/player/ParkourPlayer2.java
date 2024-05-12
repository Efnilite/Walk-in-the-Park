package dev.efnilite.ip.player;

import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.world.World;
import dev.efnilite.vilib.util.Strings;
import fr.mrmicky.fastboard.FastBoard;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.util.Vector;

import java.util.concurrent.CompletableFuture;

public class ParkourPlayer2 {

    private final Player player;
    private final PreviousData previousData;
    private final FastBoard board;
    public ParkourPlayer2(Player player) {
        this(player, new PreviousData(player));
    }

    public ParkourPlayer2(Player player, PreviousData previousData) {
        this.player = player;
        this.previousData = previousData;
        this.board = new FastBoard(player);
    }

    public static ParkourPlayer2 as(Player player) {
        return null;
    }

    public void sendToServer(String server) {
        IP.log("Sending %s to proxy server $server".formatted(player.getName()));

        var out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(IP.getPlugin(), "BungeeCord", out.toByteArray());

            IP.log("Sent %s to proxy server %s".formatted(player.getName(), server));
        } catch (ChannelNotRegisteredException ex) {
            IP.logging().stack("%s is not registered with BungeeCord".formatted(server), ex);
            player.kickPlayer("%s is not registered with BungeeCord".formatted(server));
        }
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
     * Gets the player's position.
     * @return The player's position.
     */
    public Vector getPosition() {
        return player.getLocation().toVector();
    }

    public Player getPlayer() {
        return player;
    }

    public PreviousData getPreviousData() {
        return previousData;
    }

    public FastBoard getBoard() {
        return board;
    }
}
