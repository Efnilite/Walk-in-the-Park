package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.WaveWestAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * The menu for all community-related things
 */
public class CommunityMenu extends DynamicMenu {

    public CommunityMenu() {
        registerMainItem(1, 1,
                user -> IP.getConfiguration().getFromItemData(user, "main.leaderboard").click(
                        event -> Menus.LEADERBOARDS.open(event.getPlayer())),
                ParkourOption.LEADERBOARDS::check);
    }

    public void open(Player player) {
        Menu menu = new Menu(3, "<white>Community")
                .distributeRowsEvenly()
                .animation(new WaveWestAnimation())
                .fillBackground(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        display(player, menu);
    }
}
