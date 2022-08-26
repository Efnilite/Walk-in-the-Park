package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
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
        String locale = user == null ? Option.DEFAULT_LOCALE : user.getLocale();

        PagedMenu gamemode = new PagedMenu(4, Locales.getString(player, "community.leaderboards.name"));

        Gamemode latest = null;
        List<MenuItem> items = new ArrayList<>();
        for (Gamemode gm : IP.getRegistry().getGamemodes()) {
            if (gm.getLeaderboard() == null || !gm.isVisible()) {
                continue;
            }

            Item item = gm.getItem(locale);
            items.add(item.clone()
                    .click(event -> {
                        if (gm.getName().equals("time-trial") || gm.getName().equals("duels")) {
                            Menus.SINGLE_LEADERBOARD.open(player, gm, SingleLeaderboardMenu.Sort.TIME);
                        } else {
                            Menus.SINGLE_LEADERBOARD.open(player, gm, SingleLeaderboardMenu.Sort.SCORE);
                        }
                    }));
            latest = gm;
        }

        if (items.size() == 1) {
            Menus.SINGLE_LEADERBOARD.open(player, latest, SingleLeaderboardMenu.Sort.SCORE);
            return;
        }

        gamemode
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> gamemode.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> gamemode.page(-1)))

                .item(31, Locales.getItem(player, "other.close")
                        .click(event -> Menus.COMMUNITY.open(event.getPlayer())))

                .fillBackground(Material.WHITE_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .open(player);
    }
}