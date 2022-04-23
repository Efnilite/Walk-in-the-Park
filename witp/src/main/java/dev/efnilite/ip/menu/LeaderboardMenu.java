package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.Highscore;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.WaveEastAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A class containing
 */
public class LeaderboardMenu {

    /**
     * Shows the leaderboard menu
     *
     * @param   user
     *          The user, but can be null
     *
     * @param   player
     *          The player
     */
    public static void open(Player player) {
        ParkourUser.initHighScores(); // make sure scores are enabled

        // sort high scores by value
        HashMap<UUID, Integer> sorted = Util.sortByValue(ParkourUser.highScores);
        ParkourUser.highScores = sorted;
        List<UUID> uuids = new ArrayList<>(sorted.keySet());

        // init vars
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LANG.get() : user.getLocale();
        Configuration config = IP.getConfiguration();
        PagedMenu leaderboard = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + locale + ".options.leaderboard.name")));
        List<MenuItem> items = new ArrayList<>();

        int rank = 1;
        Item base = config.getFromItemData(locale, "options.leaderboard-head");
        for (UUID uuid : uuids) {
            Highscore highscore = ParkourUser.getHighscore(uuid);
            if (highscore == null) {
                continue;
            }

            int finalRank = rank;
            Item item = base.clone()
                    .material(Material.PLAYER_HEAD)
                    .modifyName(name -> name.replace("%r", Integer.toString(finalRank))
                            .replace("%s", Integer.toString(ParkourUser.getHighestScore(uuid)))
                            .replace("%p", highscore.name)
                            .replace("%t", highscore.time)
                            .replace("%d", highscore.diff))
                    .modifyLore(line -> line.replace("%r", Integer.toString(finalRank))
                            .replace("%s", Integer.toString(ParkourUser.getHighestScore(uuid)))
                            .replace("%p", highscore.name)
                            .replace("%t", highscore.time)
                            .replace("%d", highscore.diff));

            // Player head gathering
            ItemStack stack = item.build();
            stack.setType(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) stack.getItemMeta();
            if (meta == null) {
                continue;
            }
            SkullSetter.setPlayerHead(Bukkit.getOfflinePlayer(uuid), meta);
            item.meta(meta);

            if (uuid.equals(player.getUniqueId())) {
                leaderboard.item(30, item.clone());
                item.glowing();
            }

            items.add(item);
            rank++;
        }

        leaderboard
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> leaderboard.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> leaderboard.page(-1)))

                .item(32, config.getFromItemData(locale, "general.close")
                        .click(event -> player.closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(player);
    }

}
