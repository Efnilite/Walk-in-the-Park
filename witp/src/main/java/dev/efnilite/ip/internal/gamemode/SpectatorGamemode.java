package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.menu.SpectatorMenu;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
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
        return new Item(Material.GLASS, "<#39D5AB><bold>Spectator")
                .lore("<dark_gray>Zuschauer • 观众", "<dark_gray>觀眾 • Spectateur", "<dark_gray>見物人 • Toekijker");
    }

    @Override
    public void create(Player player) {
        throw new IllegalStateException("SpectatorGamemode uses #create(Player, Session) for instance creation");
    }

    public void create(Player player, Session session) {
        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (user != null) {
            ParkourUser.unregister(user, false, false, true);
            spectator = new ParkourSpectator(player, session, user.getPreviousData());
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }

    @Override
    public void click(Player player) {
        SpectatorMenu.open(player);
    }

    @Override
    public boolean isVisible() {
        return false;
    }
}
