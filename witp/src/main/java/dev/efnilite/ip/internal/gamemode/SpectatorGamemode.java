package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.menu.SpectatorMenu;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectatorGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return IP.getConfiguration().getFromItemData(locale, "gamemodes.spectator");
    }

    @Override
    public void create(Player player) {
        throw new IllegalStateException("SpectatorGamemode uses #create(Player, Session) for instance creation");
    }

    public void create(Player player, Session session) {
        ParkourUser user = ParkourUser.getUser(player);
        if (user != null) {
            ParkourUser.unregister(user, false, false, true);
            new ParkourSpectator(player, session, user.getPreviousData());
        } else {
            new ParkourSpectator(player, session, null);
        }
    }

    @Override
    public void click(Player player) {
        SpectatorMenu.open(player);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
