package dev.efnilite.ip.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectatorGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return new Item(Material.STONE, "");
    }

    @Override
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public void create(Player player) {
        throw new IllegalAccessError("SpectatorGamemode uses #create(Player, Session) for instance creation");
    }

    public void create(Player player, Session session) {
        if (!Option.JOINING) {
            player.sendMessage(Strings.colour(IP.PREFIX + "Joining is currently disabled."));
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (user != null) {
            ParkourUser.unregister(user, false, false, true);
            spectator = new ParkourSpectator(player, session, user.previousData);
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }

    @Override
    public void click(Player player) {
        Menus.SPECTATOR.open(player);
    }

    @Override
    public boolean isVisible() {
        return false;
    }
}
