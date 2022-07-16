package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.util.Task;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvisible(true);
        player.setCollidable(false);

        sendTranslated("spectator");
        runClosestChecker();
    }

    @Override
    public void updateScoreboard() {
        if (Option.SCOREBOARD_ENABLED && board != null) {
            Leaderboard leaderboard = getSession().getGamemode().getLeaderboard();

            // scoreboard settings
            String title = Util.translate(closest.getPlayer(), Util.color(Option.SCOREBOARD_TITLE));
            List<String> lines = new ArrayList<>();

            Score top = leaderboard.getAtRank(1);
            Score rank = leaderboard.get(getUUID());

            if (top == null) {
                top = new Score("?", "?", "?", 0);
            }
            if (rank == null) {
                rank = new Score("?", "?", "?", 0);
            }

            for (String s : Option.SCOREBOARD_LINES) {
                s = Util.translate(closest.getPlayer(), s); // add support for PAPI placeholders in scoreboard
                lines.add(s.replace("%score%", Integer.toString(closest.getGenerator().getScore()))
                        .replace("%time%", closest.getGenerator().getTime())
                        .replace("%highscore%", Integer.toString(rank.score()))
                        .replace("%topscore%", Integer.toString(top.score()))
                        .replace("%topplayer%", top.name())
                        .replace("%session%" , getSessionId()));
            }

            board.updateTitle(title);
            board.updateLines(lines);
        }
    }

    /**
     * Runs a checker which checks for the closest {@link ParkourPlayer}, and updates the scoreboard, etc. accordingly.
     */
    public void runClosestChecker() {
        closestChecker = Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
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