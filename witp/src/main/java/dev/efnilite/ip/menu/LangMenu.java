package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.WaveWestAnimation;
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
     *
     * @param   disabledOptions
     *          Options which are disabled
     */
    public static void open(ParkourPlayer user, ParkourOption... disabledOptions) {
        Configuration config = IP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.language.name")));

        List<MenuItem> items = new ArrayList<>();
        for (String lang : Option.LANGUAGES.get()) {
            Item item = new Item(Material.PAPER, "<#238681><bold>" + lang);

            items.add(item
                    .glowing(user.getLocale().equals(lang))
                    .click(event -> {
                        user.setLocale(lang);
                        user.lang = lang;
                        open(user, disabledOptions);
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> style.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> style.page(-1)))

                .item(31, config.getFromItemData(user.getLocale(), "general.close")
                        .click(event -> SettingsMenu.open(user, disabledOptions)))

                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new WaveWestAnimation())
                .open(user.getPlayer());
    }

}
