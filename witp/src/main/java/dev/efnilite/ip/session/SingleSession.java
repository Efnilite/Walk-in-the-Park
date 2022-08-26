package dev.efnilite.ip.session;

import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
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

    protected Gamemode gamemode;
    protected SessionVisibility visibility = SessionVisibility.PUBLIC;
    protected final String sessionId = generateSessionId();
    protected final List<ParkourUser> muted = new ArrayList<>();
    protected final Map<UUID, ParkourPlayer> players = new LinkedHashMap<>();
    protected final Map<UUID, ParkourSpectator> spectators = new LinkedHashMap<>();

    public static Session create(@NotNull ParkourPlayer player, @NotNull Gamemode gamemode) {
        // create session
        Session session = new SingleSession();

        // add player
        session.addPlayers(player);
        session.register();

        // set gamemode
        session.setGamemode(gamemode);

        return session;
    }

    @Override
    public void updateSpectators() {
        for (ParkourSpectator spectator : spectators.values()) {
            Player bukkitPlayer =  spectator.player;
            Player watchingPlayer =  spectator.getClosest().player;

            Entity target = bukkitPlayer.getSpectatorTarget();

            if (watchingPlayer.getLocation().distance(bukkitPlayer.getLocation()) > 30) {
                if (bukkitPlayer.getGameMode() == GameMode.SPECTATOR) { // if player is a spectator
                    bukkitPlayer.setSpectatorTarget(null);
                    spectator.teleport(watchingPlayer.getLocation());
                    bukkitPlayer.setSpectatorTarget(target);
                } else { // if player isn't a spectator (bedrock)?
                    spectator.teleport(watchingPlayer.getLocation());
                }
            }
            String string = Locales.getString(bukkitPlayer, "spectator.action_bar");
            bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(string));

            spectator.updateScoreboard();
        }
    }

    @Override
    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.send(Locales.getString(player.getLocale(), "play.spectator.other_join").formatted(spectator.getName()));
            }

            this.spectators.put(spectator.getUUID(), spectator);
            spectator.sessionId = getSessionId();
        }
    }

    @Override
    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            for (ParkourPlayer player : players.values()) {
                player.send(Locales.getString(player.getLocale(), "play.spectator.other_leave").formatted(spectator.getName()));
            }

            this.spectators.remove(spectator.getUUID());
            unregister();
        }
    }

    @Override
    public boolean isAcceptingSpectators() {
        return visibility == SessionVisibility.PUBLIC;
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
            player.sessionId = getSessionId();
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

    @NotNull
    @Override
    public Gamemode getGamemode() {
        return gamemode;
    }

    @Override
    public void setGamemode(@NotNull Gamemode gamemode) {
        this.gamemode = gamemode;
    }

    @Override
    public void setMuted(@NotNull ParkourUser user, boolean muted) {
        if (muted) {
            this.muted.add(user);
        } else {
            this.muted.remove(user);
        }
    }

    @Override
    public boolean isMuted(@NotNull ParkourUser user) {
        return muted.contains(user);
    }
}
