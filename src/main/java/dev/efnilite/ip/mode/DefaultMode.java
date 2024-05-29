package dev.efnilite.ip.mode;

import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default parkour mode
 */
public class DefaultMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "default";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.default");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("Joining is currently disabled.");
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof DefaultMode) {
            return;
        }

        player.closeInventory();

        Session.create(ParkourGenerator::new, null, null, player);
    }
}
