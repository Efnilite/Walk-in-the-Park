package dev.efnilite.witp.internal.gamemode;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.Gamemode;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

;

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
        return WITP.getConfiguration().getFromItemData(locale, "gamemodes.default");
    }

    @Override
    public void handleItemClick(Player player, ParkourUser user, Menu previousMenu) {
        player.closeInventory();
        ParkourUser.unregister(user, false, false, true);
        ParkourPlayer pp = ParkourPlayer.register(player);
        WITP.getDivider().generate(pp);
    }
}
