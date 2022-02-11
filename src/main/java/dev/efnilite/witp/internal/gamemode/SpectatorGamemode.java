package dev.efnilite.witp.internal.gamemode;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.PagedMenu;
import dev.efnilite.fycore.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.inventory.item.MenuItem;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.SkullSetter;
import dev.efnilite.witp.ParkourMenu;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.Gamemode;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Unicodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SpectatorGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return WITP.getConfiguration().getFromItemData(locale, "gamemodes.spectator");
    }

    @Override
    public void handleItemClick(Player player, ParkourUser user, Menu previousMenu) {
        PagedMenu spectator = new PagedMenu(4, "<white>Select a player");
        player.closeInventory();

        List<MenuItem> display = new ArrayList<>();

        for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
            Player bukkitPlayer = pp.getPlayer();
            if (bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                continue;
            }

            Item item = WITP.getConfiguration().getFromItemData(user.locale,
                    "gamemodes.spectator-head", bukkitPlayer.getName(), bukkitPlayer.getName());

            item.material(Material.PLAYER_HEAD);

            ItemStack stack = item.build();
            stack.setType(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) stack.getItemMeta();
            if (meta == null) {
                continue;
            }
            SkullSetter.setPlayerHead(bukkitPlayer, meta);
            item.meta(meta);

            display.add(item.click((menu, event) -> {
                if (ParkourUser.getActivePlayers().contains(pp)) {
                    new ParkourSpectator(user, pp, user.getPreviousData());
                }
            }));
        }

        spectator
                .item(31, WITP.getConfiguration().getFromItemData(user.locale, "general.close")
                .click((menu, event) -> {
                    try { // todo make method out of this
                        player.closeInventory();
                        ParkourUser.unregister(user, false, false, true);
                        ParkourPlayer pp = ParkourPlayer.register(player, user.getPreviousData());
                        WITP.getDivider().generate(pp);
                        ParkourMenu.openMainMenu(pp);
                    } catch (IOException | SQLException ex) {
                        Logging.stack("Error while joining player " + player.getName(),
                                "Please try again or report this error to the developer!", ex);
                    }
                }));

        spectator
                .displayRows(0, 1)
                .addToDisplay(display)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click((menu, event) -> spectator.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click((menu, event) -> spectator.page(-1)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleInAnimation())
                .open(player);
    }
}
