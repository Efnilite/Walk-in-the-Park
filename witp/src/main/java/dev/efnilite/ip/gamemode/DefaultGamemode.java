package dev.efnilite.ip.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session2;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The default parkour gamemode
 */
public class DefaultGamemode implements Gamemode {

    private final Leaderboard leaderboard = new Leaderboard(getName());

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.default");
    }

    @Override
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Option.JOINING) {
            player.sendMessage(Strings.colour(IP.PREFIX + "Joining is currently disabled."));
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.getSession().getGamemode() instanceof DefaultGamemode) {
            return;
        }
        player.closeInventory();

        Session2.Builder.create()
                .addPlayers(ParkourUser.register(player))
                .complete();
    }

    @Override
    public void click(Player player) {
        create(player);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
