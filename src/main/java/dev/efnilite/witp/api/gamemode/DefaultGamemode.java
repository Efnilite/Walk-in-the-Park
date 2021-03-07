package dev.efnilite.witp.api.gamemode;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Verbose;
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
            if (user instanceof ParkourSpectator) {
                ParkourSpectator spectator = (ParkourSpectator) user;
                spectator.getWatching().removeSpectators(spectator);
            } else if (user instanceof ParkourPlayer) {
                user.sendTranslated("cant-do");
                return;
            }
            ParkourPlayer pp = ParkourPlayer.register(player);
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            Verbose.error("Error while trying to register player" + player.getName());
        }
    }
}
