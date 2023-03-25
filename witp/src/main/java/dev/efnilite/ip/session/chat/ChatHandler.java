package dev.efnilite.ip.session.chat;

import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session2;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles chat-related events
 */
public class ChatHandler implements Listener {

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        ParkourUser user = ParkourUser.getUser(sender);

        if (!Option.OPTIONS_ENABLED.get(ParkourOption.CHAT) || user == null) {
            return;
        }

        Session2 session = user.session;

        if (session.muted.contains(user)) {
            return;
        }

        switch (user.chatType) {
            case LOBBY_ONLY -> {
                event.setCancelled(true);

                // send it to all players
                for (ParkourPlayer pp : session.getPlayers()) {
                    pp.sendTranslated("settings.chat.formats.lobby", sender.getName(), event.getMessage());
                }
                for (ParkourSpectator sp : session.getSpectators()) {
                    sp.sendTranslated("settings.chat.formats.lobby", sender.getName(), event.getMessage());
                }
            }
            case PLAYERS_ONLY -> {
                event.setCancelled(true);

                // send it to all users
                for (ParkourPlayer pp : session.getPlayers()) {
                    pp.sendTranslated("settings.chat.formats.players", sender.getName(), event.getMessage());
                }
            }
        }
    }
}