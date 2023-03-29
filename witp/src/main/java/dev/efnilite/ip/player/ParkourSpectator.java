package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.event.ParkourSpectateEvent;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.util.Strings;
import dev.efnilite.vilib.util.Task;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
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

    public ParkourPlayer closest;
    protected BukkitTask closestChecker;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, previousData);

        new ParkourSpectateEvent(this).call();

        this.session = session;
        this.closest = session.getPlayers().get(0);

        player.teleport(closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SPECTATOR);

        player.setAllowFlight(true);
        player.setFlying(true);

        // bedrock has no spectator mode, so just make the player invisible
        if (Util.isBedrockPlayer(player)) {
            player.setInvisible(true);
            player.setCollidable(false);
        }

        sendTranslated("play.spectator.join");
        runClosestChecker();
    }

    public void update() {
        Player bukkitPlayer = player;
        Player watchingPlayer = closest.player;

        Entity target = bukkitPlayer.getSpectatorTarget();

        if (watchingPlayer.getLocation().distanceSquared(bukkitPlayer.getLocation()) > 10000) {
            if (bukkitPlayer.getGameMode() == GameMode.SPECTATOR) { // if player is a spectator
                bukkitPlayer.setSpectatorTarget(null);
                teleport(watchingPlayer.getLocation());
                bukkitPlayer.setSpectatorTarget(target);
            } else { // if player isn't a spectator (bedrock)?
                teleport(watchingPlayer.getLocation());
            }
        }
        String string = Strings.colour(Locales.getString(bukkitPlayer, "play.spectator.action_bar"));
        bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(string));

        player.setGameMode(GameMode.SPECTATOR);
        updateScoreboard();
    }

    public void updateScoreboard() {
        if (!Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            return;
        }

        // board can be null a few ticks after on player leave
        if (board == null) {
            return;
        }
        ParkourPlayer player = closest;
        ParkourGenerator generator = player.generator;

        Leaderboard leaderboard = generator.getGamemode().getLeaderboard();

        String title = Strings.colour(Util.translate(player.player, Locales.getString(player.getLocale(), "scoreboard.title")));
        List<String> lines = new ArrayList<>();

        Score top = null, rank = null;
        if (leaderboard != null) {

            // only get score at rank if lines contains variables
            if (Colls.filter(line -> line.contains("topscore") || line.contains("topplayer"), lines).size() > 0) {
                top = leaderboard.getScoreAtRank(1);
            }

            rank = leaderboard.get(player.getUUID());
        }

        // set generic score if score is not found
        top = top == null ? new Score("?", "?", "?", 0) : top;
        rank = rank == null ? new Score("?", "?", "?", 0) : rank;


        // update lines
        for (String line : Colls.map(Strings::colour, Locales.getStringList(player.getLocale(), "scoreboard.lines"))) {
            line = Util.translate(player.player, line); // add support for PAPI placeholders in scoreboard

            lines.add(line.replace("%score%", Integer.toString(generator.score))
                    .replace("%time%", generator.getTime())
                    .replace("%highscore%", Integer.toString(rank.score()))
                    .replace("%topscore%", Integer.toString(top.score()))
                    .replace("%topplayer%", top.name())
                    .replace("%session%", session.getPlayers().get(0).getName()));
        }

        board.updateTitle(title.replace("%score%", Integer.toString(generator.score))
                .replace("%time%", generator.getTime())
                .replace("%highscore%", Integer.toString(rank.score()))
                .replace("%topscore%", Integer.toString(top.score()))
                .replace("%topplayer%", top.name())
                .replace("%session%", session.getPlayers().get(0).getName()));
        board.updateLines(lines);
    }

    /**
     * Runs a checker which checks for the closest {@link ParkourPlayer}, and updates the scoreboard, etc. accordingly.
     */
    public void runClosestChecker() {
        closestChecker = Task.create(IP.getPlugin()).async().execute(() -> {
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

            this.closest = closest == null ? session.getPlayers().get(0) : closest;
        }).repeat(10).run();
    }

    /**
     * Stops the closest checker runnable.
     */
    public void unregister() {
        closestChecker.cancel();
        session.removeSpectators(this);

        player.setInvisible(false);
    }
}