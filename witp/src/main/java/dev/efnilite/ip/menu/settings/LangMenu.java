package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.WaveEastAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class LangMenu {

    /**
     * Opens the language menu
     *
     * @param   user
     *          The ParkourPlayer instance
     */
    public void open(ParkourPlayer user) {
        Configuration config = IP.getConfiguration();

        if (user == null) {
            return;
        }

        // init menu
        PagedMenu style = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.language.name")));

        List<MenuItem> items = new ArrayList<>();
        for (String lang : Option.LANGUAGES) {
            Item item = new Item(Material.PAPER, "<#238681><bold>" + config.getString("lang", "messages." + lang + ".name"));

            items.add(item
                    .glowing(user.getLocale().equals(lang))
                    .click(event -> {
                        user.setLocale(lang);
                        user.lang = lang;
                        Menus.SETTINGS.open(event.getPlayer());
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> style.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> style.page(-1)))

                .item(31, config.getFromItemData(user, "general.close")
                        .click(event -> Menus.SETTINGS.open(event.getPlayer())))

                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(user.getPlayer());
    }

}
