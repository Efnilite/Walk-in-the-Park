package dev.efnilite.witp.api.gamemode;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;

public class DefaultGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull ItemStack getItem(String locale) {
        return WITP.getConfiguration().getFromItemData(locale, "gamemodes.default");
    }

    @Override
    public void handleItemClick(Player player, ParkourUser user, InventoryBuilder builder) {
        try {
            player.closeInventory();
            ParkourUser.unregister(user, false, false, true);
            ParkourPlayer pp = ParkourPlayer.register(player, user.getPreviousData());
            WITP.getDivider().generate(pp);
        } catch (IOException | SQLException ex) {
            Logging.stack("Error while joining player " + player.getName(),
                    "Please try again or report this error to the developer!", ex);
        }
    }
}
