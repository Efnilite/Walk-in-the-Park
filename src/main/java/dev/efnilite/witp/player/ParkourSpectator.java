package dev.efnilite.witp.player;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.session.Session;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class for spectators.
 *
 * @author Efnilite
 */
public class ParkourSpectator extends ParkourUser {

    protected final ParkourGenerator watching;

    public ParkourSpectator(@NotNull ParkourUser player, @NotNull ParkourPlayer watching, @Nullable PreviousData previousData) {
        super(player.getPlayer(), previousData);
        Logging.verbose("New ParkourSpectator init " + this.player.getName());

        this.locale = player.locale;
        this.watching = watching.getGenerator();

        // Unregister if player is already active
        if (player instanceof ParkourPlayer) {
            unregister(player, false, false, true);
        } else if (player instanceof ParkourSpectator) {
            ParkourSpectator spectator = (ParkourSpectator) player;
            player.getSession().removeSpectators(spectator);
        }

        this.player.setGameMode(GameMode.SPECTATOR);
        this.player.teleport(watching.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

        getSession().addSpectators(this);
        sendTranslated("spectator");
    }

    @Override
    public void updateScoreboard() {
        if (Option.SCOREBOARD.get() && board != null) {
            board.updateTitle(Util.color(Option.SCOREBOARD_TITLE.get()));
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES; // doesn't use configoption
            if (lines == null) {
                Logging.error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = ParkourPlayer.getHighScoreValue(watching.getPlayer().getUUID());
            UUID one = ParkourPlayer.getAtPlace(1);
            Integer top = 0;
            Highscore highscore = null;
            if (one != null) {
                top = ParkourPlayer.getHighScoreValue(one);
                highscore = scoreMap.get(one);
            }
            for (String s : lines) {
                list.add(s.replace("%score%", Integer.toString(watching.getScore()))
                        .replace("%time%", watching.getTime())
                        .replace("%highscore%", rank != null ? rank.toString() : "0")
                        .replace("%topscore%", top != null ? top.toString() : "0")
                        .replace("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A")
                        .replace("%session%" , getSession().getSessionId()));
            }
            board.updateLines(list);
        }
    }

    @Override
    public Session getSession() {
        return watching.getPlayer().getSession();
    }

    /**
     * Get the {@link ParkourGenerator} of the spectated player.
     *
     * @return the generator of the player that is being spectated.
     */
    public ParkourPlayer getWatching() {
        return watching.getPlayer();
    }
}