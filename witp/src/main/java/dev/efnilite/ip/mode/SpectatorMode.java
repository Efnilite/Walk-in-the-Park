package dev.efnilite.ip.mode;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorMode implements Mode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return null;
    }

    @Override
    @Nullable
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public void create(Player player) {
        Menus.SPECTATOR.open(player);
    }

    public void create(Player player, Session session) {
        if (!Option.JOINING) {
            player.sendMessage(Strings.colour("<red><bold>Joining is currently disabled."));
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (user != null) {
            ParkourUser.unregister(user, false, false);
            spectator = new ParkourSpectator(player, session, user.previousData);
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }
}
