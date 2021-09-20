package dev.efnilite.witp.player;

import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class for spectators
 */
public class ParkourSpectator extends ParkourUser {

    protected final ParkourGenerator watching;

    public ParkourSpectator(@NotNull ParkourUser player, @NotNull ParkourPlayer watching) {
        super(player.getPlayer());
        Verbose.verbose("New ParkourSpectator init " + this.player.getName());
        this.locale = player.locale;

        if (player instanceof ParkourPlayer) {
            try {
                ParkourPlayer.unregister(player, false, false, true);
            } catch (IOException | InvalidStatementException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to unregister");
            }
        } else if (player instanceof ParkourSpectator) {
            ParkourSpectator spectator = (ParkourSpectator) player;
            spectator.watching.removeSpectators(spectator);
        }
        users.put(this.player.getName(), this);

        this.watching = watching.getGenerator();
        this.player.setGameMode(GameMode.SPECTATOR);
        watching.getGenerator().addSpectator(this);
        this.player.teleport(watching.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        sendTranslated("spectator");
    }

    /**
     * Checks the distance between the person the spectator is watching and the spectator.
     * If the distance is more than 30 blocks, the player gets teleported back.
     */
    public void checkDistance() {
        if (watching.getPlayer().getLocation().distance(player.getLocation()) > 30) {
            player.teleport(watching.getPlayer().getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getTranslated("spectator-bar")));
    }

    @Override
    public void updateScoreboard() {
        if (Option.SCOREBOARD) {
            board.updateTitle(Option.SCOREBOARD_TITLE);
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES;
            if (lines == null) {
                Verbose.error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = ParkourPlayer.getHighScoreValue(watching.getPlayer().uuid);
            UUID one = ParkourPlayer.getAtPlace(1);
            Integer top = 0;
            Highscore highscore = null;
            if (one != null) {
                top = ParkourPlayer.getHighScoreValue(one);
                highscore = scoreMap.get(one);
            }
            for (String s : lines) {
                list.add(s.replaceAll("%score%", Integer.toString(watching.score))
                        .replaceAll("%time%", watching.time)
                        .replaceAll("%highscore%", rank != null ? rank.toString() : "0")
                        .replaceAll("%topscore%", top != null ? top.toString() : "0")
                        .replaceAll("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            }
            board.updateLines(list);
        }
    }

    public ParkourGenerator getWatching() {
        return watching;
    }
}