package dev.efnilite.ip.internal.gamemode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
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
    public void handleItemClick(Player player, ParkourUser user, Menu previousMenu) {
        player.closeInventory();

        if (user instanceof ParkourPlayer) {
            ParkourPlayer pp = (ParkourPlayer) user;
            if (pp.getGenerator() instanceof DefaultGenerator) {
                return;
            }
        }

        ParkourPlayer pp = ParkourPlayer.register(player);
        IP.getDivider().generate(pp);
    }
}
