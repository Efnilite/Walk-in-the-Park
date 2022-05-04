package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.menu.SpectatorMenu;
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
    public void join(Player player) {
        SpectatorMenu.open(player);
    }

    @Override
    public boolean isMultiplayer() {
        return false;
    }
}
