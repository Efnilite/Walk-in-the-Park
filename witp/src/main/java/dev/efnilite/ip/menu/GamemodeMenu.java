package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class GamemodeMenu {

    /**
     * Opens the gamemode menu
     *
     * @param   user
     *          The ParkourUser instance
     */
    public static void open(ParkourUser user) {
        IP.getRegistry().close(); // prevent new registrations once a player has opened the gm menu

        Configuration config = IP.getConfiguration();
        PagedMenu gamemode = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.gamemode.name")));

        List<MenuItem> items = new ArrayList<>();
        for (Gamemode gm : IP.getRegistry().getGamemodes()) {
            Item item = gm.getItem(user.getLocale());
            items.add(new Item(item.getMaterial(), item.getName())
                    .click(event -> gm.handleItemClick(user.getPlayer(), user, event.getMenu())));
        }

        gamemode
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> gamemode.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> gamemode.page(-1)))

                .item(31, config.getFromItemData(user.getLocale(), "general.close")
                        .click(event -> user.getPlayer().closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(user.getPlayer());
    }

}
