package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Configuration;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.util.Stopwatch;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Menu for a single leaderboard
 */
public class SingleLeaderboardMenu {

    public void open(Player player, Gamemode gamemode, Sort sort) {
        Leaderboard leaderboard = gamemode.getLeaderboard();

        // init vars
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LOCALE : user.getLocale();
        Configuration config = IP.getConfiguration();
        PagedMenu menu = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + locale + ".options.leaderboard.name")));
        List<MenuItem> items = new ArrayList<>();

        int rank = 1;
        Item base = config.getFromItemData(locale, "options.leaderboard-head");

        Map<UUID, Score> sorted = sort.sort(leaderboard.getScores());

        for (UUID uuid : sorted.keySet()) {
            Score score = sorted.get(uuid);

            if (score == null) {
                continue;
            }

            int finalRank = rank;
            Item item = base.clone()
                    .material(Material.PLAYER_HEAD)
                    .modifyName(name -> name.replace("%r", Integer.toString(finalRank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()))
                    .modifyLore(line -> line.replace("%r", Integer.toString(finalRank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()));

            // Player head gathering
            ItemStack stack = item.build();
            stack.setType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (!Util.isBedrockPlayer(player)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);

                if (op.getName() != null && !op.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(Bukkit.getOfflinePlayer(uuid), meta);
                        item.meta(meta);
                    }
                }
            }

            if (uuid.equals(player.getUniqueId())) {
                menu.item(30, item.clone());
                item.glowing();
            }

            items.add(item);
            rank++;
        }

        // get next sorting type
        Sort next = switch (sort) {
            case SCORE -> Sort.TIME;
            case TIME -> Sort.DIFFICULTY;
            default -> Sort.SCORE;
        };

        List<String> values = IP.getConfiguration().getFile("items").getStringList("locale." + locale + ".options.leaderboard-sort.values");
        String name = switch (next) {
            case SCORE -> values.get(0);
            case TIME -> values.get(1);
            case DIFFICULTY -> values.get(2);
        };

        menu
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> menu.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> menu.page(-1)))

                .item(31, config.getFromItemData(locale, "options.leaderboard-sort", name.toLowerCase())
                        .click(event -> open(player, gamemode, next)))

                .item(32, config.getFromItemData(locale, "general.close")
                        .click(event -> Menus.COMMUNITY.open(event.getPlayer())))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .open(player);
    }

    public enum Sort {

        SCORE {
            @Override
            Map<UUID, Score> sort(Map<UUID, Score> scores) {
                return scores
                        .entrySet()
                        .stream()
                        .sorted((o1, o2) -> o2.getValue().score() - o1.getValue().score()) // reverse natural order
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
            }
        },
        TIME {
            @Override
            Map<UUID, Score> sort(Map<UUID, Score> scores) {
                return scores
                        .entrySet()
                        .stream()
                        .sorted((o1, o2) -> {
                            long one = Stopwatch.toMillis(o1.getValue().time());
                            long two = Stopwatch.toMillis(o2.getValue().time());

                            return Math.toIntExact(one - two); // natural order (lower == better)
                        }) // reverse natural order
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
            }
        },
        DIFFICULTY {
            @Override
            Map<UUID, Score> sort(Map<UUID, Score> scores) {
                return scores
                        .entrySet()
                        .stream()
                        .sorted((o1, o2) -> {
                            double one = Double.parseDouble(o1.getValue().difficulty());
                            double two = Double.parseDouble(o2.getValue().difficulty());

                            return (int) (100 * (two - one)); // reverse natural order (higher == better)
                        }) // reverse natural order
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
            }
        };

        abstract Map<UUID, Score> sort(Map<UUID, Score> scores);
    }
}