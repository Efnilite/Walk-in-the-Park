package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.menu.SpectatorMenu;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
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
    public void handleItemClick(Player player, ParkourUser user, Menu previousMenu) {
        SpectatorMenu.open(player);
    }

    @Override
    public boolean isMultiplayer() {
        return false;
    }
}
