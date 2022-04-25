package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SingleplayerMenu {

    /**
     * Opens the gamemode menu
     *
     * @param   player
     *          The player
     */
    public static void open(Player player) {
        IP.getRegistry().close(); // prevent new registrations once a player has opened the gm menu

        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LANG.get() : user.getLocale();

        Configuration config = IP.getConfiguration();
        PagedMenu gamemode = new PagedMenu(4, "<white>Singleplayer" +
                ChatColor.stripColor(config.getString("items", "locale." + locale + ".options.gamemode.name")));

        List<MenuItem> items = new ArrayList<>();
        for (Gamemode gm : IP.getRegistry().getGamemodes()) {
            Item item = gm.getItem(locale);
            items.add(new Item(item.getMaterial(), item.getName())
                    .click(event -> gm.handleItemClick(player, null, event.getMenu())));
        }

        gamemode
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> gamemode.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> gamemode.page(-1)))

                .item(31, config.getFromItemData(locale, "general.close")
                        .click(event -> MainMenu.open(event.getPlayer())))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(player);
    }

}
