package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.MultiMode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The menu to select single player modes
 */
public class SingleMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        List<Mode> modes = IP.getRegistry().getModes();

        if (modes.size() == 1) {
            modes.get(0).create(player);
            return;
        }

        List<MenuItem> items = new ArrayList<>();
        for (Mode mode : modes) {
            boolean permissions = Option.PERMISSIONS && !player.hasPermission("ip.gamemode." + mode.getName());

            Item item = mode.getItem(locale);

            if (permissions || mode instanceof MultiMode || item == null) {
                continue;
            }

            items.add(item.clone().click(event -> {
                if (user == null || Duration.between(user.joined, Instant.now()).toSeconds() > 3) {
                    mode.create(player);
                }
            }));
        }

        PagedMenu mode = new PagedMenu(3, Locales.getString(player, "play.single.name"));
        mode.displayRows(0, 1)
                .addToDisplay(items)
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT).click(event -> mode.page(1)))
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT).click(event -> mode.page(-1)))
                .item(22, Locales.getItem(player, "other.close").click(event -> Menus.PLAY.open(event.getPlayer())))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE)
                .open(player);
    }

}
