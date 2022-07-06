package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.hook.PlaceholderHook;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.util.Task;
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

        this.session = session;
        this.closest = session.getPlayers().get(0);
        this.player.setGameMode(GameMode.SPECTATOR);
        this.player.teleport(closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        this.player.setFlying(true);

        sendTranslated("spectator");
        runClosestChecker();
    }

    @Override
    public void updateScoreboard() {
        if (Option.SCOREBOARD.get() && board != null) {
            String title = Util.color(Option.SCOREBOARD_TITLE.get());
            title = PlaceholderHook.translate(closest.getPlayer(), title); // add support for PAPI placeholders in scoreboard

            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES; // doesn't use configoption
            if (lines == null) {
                IP.logging().error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = ParkourPlayer.getHighScoreValue(closest.getUUID());
            UUID one = ParkourPlayer.getAtPlace(1);
            int top = 0;
            Score highscore = null;
            if (one != null) {
                top = ParkourPlayer.getHighScoreValue(one);
                highscore = topScores.get(one);
            }
            for (String s : lines) {
                s = PlaceholderHook.translate(closest.getPlayer(), s); // add support for PAPI placeholders in scoreboard
                list.add(s.replace("%score%", Integer.toString(closest.getGenerator().getScore()))
                        .replace("%time%", closest.getGenerator().getTime())
                        .replace("%highscore%", rank.toString())
                        .replace("%topscore%", Integer.toString(top))
                        .replace("%topplayer%", highscore != null && highscore.name() != null ? highscore.name() : "N/A")
                        .replace("%session%" , getSessionId()));
            }

            board.updateTitle(title);
            board.updateLines(list);
        }
    }

    /**
     * Runs a checker which checks for the closest {@link ParkourPlayer}, and updates the scoreboard, etc. accordingly.
     */
    public void runClosestChecker() {
        closestChecker = Task.create(IP.getPlugin())
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