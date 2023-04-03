package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboards menu
 */
public class LeaderboardsMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        PagedMenu menu = new PagedMenu(3, Locales.getString(player, "%s.name".formatted(ParkourOption.LEADERBOARDS.path)));

        Mode latest = null;
        List<MenuItem> items = new ArrayList<>();
        for (Mode mode : Registry.getModes()) {
            Leaderboard leaderboard = mode.getLeaderboard();
            Item item = mode.getItem(locale);

            if (leaderboard == null || item == null) {
                continue;
            }

            items.add(item.clone().click(event -> Menus.SINGLE_LEADERBOARD.open(player, mode, leaderboard.sort)));
            latest = mode;
        }

        if (items.size() == 1) {
            Menus.SINGLE_LEADERBOARD.open(player, latest, SingleLeaderboardMenu.Sort.SCORE);
            return;
        }

        menu.displayRows(0, 1)
                .addToDisplay(items)
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT).click(event -> menu.page(1)))
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT).click(event -> menu.page(-1)))
                .item(22, Locales.getItem(player, "other.close").click(event -> Menus.COMMUNITY.open(event.getPlayer())))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.WHITE_STAINED_GLASS_PANE)
                .open(player);
    }
}