package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.WaveWestAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * The menu for all community-related things
 */
public class CommunityMenu extends DynamicMenu {

    public CommunityMenu() {
        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "community.leaderboards.item").click(event -> Menus.LEADERBOARDS.open(event.getPlayer())), ParkourOption.LEADERBOARDS::mayPerform);

        registerMainItem(2, 10, (player, user) -> Locales.getItem(player, "other.close").click(event -> event.getPlayer().closeInventory()), player -> true);
    }

    public void open(Player player) {
        Menu menu = new Menu(3, Locales.getString(player, "community.name"))
                .distributeRowsEvenly()
                .animation(new WaveWestAnimation())
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        display(player, menu);
    }
}
