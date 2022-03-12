package dev.efnilite.witp.session;

import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generic implementation of {@link Session}. This is for one player.
 *
 * @author Efnilite
 */
public class SingleSession implements Session {

    private SessionVisibility visibility = SessionVisibility.PUBLIC;
    private final String sessionId = generateSessionId();
    private final Map<UUID, ParkourPlayer> players = new HashMap<>();
    private final Map<UUID, ParkourSpectator> spectators = new HashMap<>();

    @Override
    public void updateSpectators() {
        for (ParkourSpectator spectator : spectators.values()) {
            Player bukkitPlayer =  spectator.getPlayer();
            Player watchingPlayer =  spectator.getClosest().getPlayer();

            Entity target = bukkitPlayer.getSpectatorTarget();

            if (watchingPlayer.getLocation().distance(bukkitPlayer.getLocation()) > 30) {
                bukkitPlayer.setSpectatorTarget(null);
                spectator.teleport(watchingPlayer.getLocation());
                bukkitPlayer.setSpectatorTarget(target);
            }
            bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(spectator.getTranslated("spectator-bar")));

            spectator.updateScoreboard();
        }
    }

    @Override
    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.sendTranslated("spectator-join", spectator.getPlayer().getName());
            }

            this.spectators.put(spectator.getUUID(), spectator);
            spectator.setSessionId(getSessionId());
        }
    }

    @Override
    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.sendTranslated("spectator-leave", spectator.getPlayer().getName());
            }

            this.spectators.remove(spectator.getUUID());
            unregister();
        }
    }

    @Override
    public boolean isAcceptingSpectators() {
        return true;
    }

    @Override
    @NotNull
    public List<ParkourSpectator> getSpectators() {
        return new ArrayList<>(spectators.values());
    }

    @Override
    public void addPlayers(ParkourPlayer... players) {
        for (ParkourPlayer player : players) {
            this.players.put(player.getUUID(), player);
        }
    }

    @Override
    public void removePlayers(ParkourPlayer... players) {
        for (ParkourPlayer player : players) {
            this.players.remove(player.getUUID());
        }
    }

    @Override
    public boolean isAcceptingPlayers() {
        return false;
    }

    @Override
    @NotNull
    public List<ParkourPlayer> getPlayers() {
        return new ArrayList<>(players.values());
    }

    @Override
    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void setVisibility(SessionVisibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public SessionVisibility getVisibility() {
        return visibility;
    }
}
