package dev.efnilite.witp.player;

import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.witp.IP;
import dev.efnilite.witp.hook.PlaceholderHook;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.player.data.PreviousData;
import dev.efnilite.witp.session.Session;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class for spectators of a Session.
 *
 * @author Efnilite
 */
public class ParkourSpectator extends ParkourUser {

    protected ParkourPlayer closest;
    protected BukkitTask closestChecker;
    protected final Session session;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, previousData);
        Logging.verbose("New ParkourSpectator init " + this.player.getName());

        this.session = session;
        this.closest = session.getPlayers().get(0);
        this.player.setGameMode(GameMode.SPECTATOR);
        this.player.teleport(closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        this.player.setFlying(true);

        session.addSpectators(this);
        sendTranslated("spectator");
        runClosestChecker();
    }

    /**
     * Registers a ParkourSpectator.
     *
     * @param   player
     *          The player that will become the ParkourSpectator
     *
     * @param   session
     *          The session to spectate
     *
     * @return the created instance.
     */
    public static ParkourSpectator spectateSession(@NotNull Player player, @NotNull Session session) {
        ParkourUser user = getUser(player);
        if (user != null) {
            ParkourUser.unregister(user, false, false, true);
            return new ParkourSpectator(player, session, user.getPreviousData());
        } else {
            return new ParkourSpectator(player, session, null);
        }
    }

    @Override
    public void updateScoreboard() {
        if (Option.SCOREBOARD.get() && board != null) {
            String title = Util.color(Option.SCOREBOARD_TITLE.get());
            title = translatePlaceholders(closest, title); // add support for PAPI placeholders in scoreboard

            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES; // doesn't use configoption
            if (lines == null) {
                Logging.error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = ParkourPlayer.getHighScoreValue(closest.getUUID());
            UUID one = ParkourPlayer.getAtPlace(1);
            Integer top = 0;
            Highscore highscore = null;
            if (one != null) {
                top = ParkourPlayer.getHighScoreValue(one);
                highscore = scoreMap.get(one);
            }
            for (String s : lines) {
                s = translatePlaceholders(closest, s); // add support for PAPI placeholders in scoreboard
                list.add(s.replace("%score%", Integer.toString(closest.getGenerator().getScore()))
                        .replace("%time%", closest.getGenerator().getTime())
                        .replace("%highscore%", rank != null ? rank.toString() : "0")
                        .replace("%topscore%", top != null ? top.toString() : "0")
                        .replace("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A")
                        .replace("%session%" , getSessionId()));
            }

            board.updateTitle(title);
            board.updateLines(list);
        }
    }

    private String translatePlaceholders(ParkourUser user, String string) {
        if (IP.getPlaceholderHook() == null) {
            return string;
        }
        return PlaceholderHook.translate(user.getPlayer(), string);
    }

    /**
     * Runs a checker which checks for the closest {@link ParkourPlayer}, and updates the scoreboard, etc. accordingly.
     */
    public void runClosestChecker() {
        closestChecker = new Task()
                .async()
                .execute(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (session.getPlayers().size() < 2) { // only update if there is more than 1 player
                            return;
                        }

                        double leastDistance = 1000000;
                        ParkourPlayer closest = null;

                        for (ParkourPlayer pp : session.getPlayers()) {
                            double distance = pp.getLocation().distance(player.getLocation());
                            if (distance < leastDistance) {
                                closest = pp;
                                leastDistance = distance;
                            }
                        }

                        setClosest(closest == null ? session.getPlayers().get(0) : closest);

                        updateVisualTime(getClosest().selectedTime);
                    }
                })
                .repeat(10)
                .run();
    }

    /**
     * Stops the closest checker runnable.
     */
    public void stopClosestChecker() {
        closestChecker.cancel();
    }

    public void setClosest(ParkourPlayer closest) {
        this.closest = closest;
    }

    public ParkourPlayer getClosest() {
        return closest;
    }

    @Override
    public Session getSession() {
        return session;
    }
}