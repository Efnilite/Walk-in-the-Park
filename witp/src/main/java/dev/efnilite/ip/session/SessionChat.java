package dev.efnilite.ip.session;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles session chat-related events
 */
public class SessionChat implements Listener {

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        if (!Option.OPTIONS_ENABLED.get(ParkourOption.CHAT)) {
            return;
        }

        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        Session session = user.session;

        if (session.muted.contains(user)) {
            return;
        }

        switch (user.chatType) {
            case LOBBY_ONLY -> {
                event.setCancelled(true);

                // send it to all players
                for (ParkourPlayer other : session.getPlayers()) {
                    other.sendTranslated("settings.chat.formats.lobby", player.getName(), event.getMessage());
                }
                for (ParkourSpectator other : session.getSpectators()) {
                    other.sendTranslated("settings.chat.formats.lobby", player.getName(), event.getMessage());
                }
            }
            case PLAYERS_ONLY -> {
                event.setCancelled(true);

                // send it to all users
                for (ParkourPlayer other : session.getPlayers()) {
                    other.sendTranslated("settings.chat.formats.players", player.getName(), event.getMessage());
                }
            }
        }
    }

    /**
     * An enum for all available chat types that a player can select while playing
     */
    public enum ChatType {

        LOBBY_ONLY, PLAYERS_ONLY, PUBLIC

    }
}