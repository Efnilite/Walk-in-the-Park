package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class LangMenu {

    /**
     * Opens the language menu
     *
     * @param user The ParkourPlayer instance
     */
    public void open(ParkourPlayer user) {
        if (user == null) {
            return;
        }

        PagedMenu style = new PagedMenu(3, Locales.getString(user.locale, "settings.lang.name"));

        List<MenuItem> items = new ArrayList<>();
        for (String lang : Locales.locales.keySet()) {
            Item item = new Item(Material.PAPER, "<#238681><bold>" + Locales.getString(lang, "name"));

            items.add(item.glowing(user.locale.equals(lang)).click(event -> {
                user.locale = lang;
                user._locale = lang;
                Menus.SETTINGS.open(event.getPlayer());
            }));
        }

        style.displayRows(0, 1).addToDisplay(items)
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>»").click(event -> style.page(1)))
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>«").click(event -> style.page(-1)))
                .item(22, Locales.getItem(user.locale, "other.close").click(event -> Menus.SETTINGS.open(event.getPlayer())))
                .fillBackground(ParkourUser.isBedrockPlayer(user.player) ? Material.AIR : Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .open(user.player);
    }

}
