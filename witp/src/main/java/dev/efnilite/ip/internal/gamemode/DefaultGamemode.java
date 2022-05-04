package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The default parkour gamemode
 */
public class DefaultGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return IP.getConfiguration().getFromItemData(locale, "gamemodes.default");
    }

    @Override
    public void join(Player player) {
        player.closeInventory();

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.getGenerator() instanceof DefaultGenerator) {
            return;
        }

        ParkourUser.joinDefault(player);
    }

    @Override
    public boolean isMultiplayer() {
        return false;
    }
}
