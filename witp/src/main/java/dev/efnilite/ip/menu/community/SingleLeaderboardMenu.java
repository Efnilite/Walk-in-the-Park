package dev.efnilite.ip.menu.community;

import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.player.Score;
import dev.efnilite.ip.util.Stopwatch;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Bukkit;
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
        String locale = user == null ? (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.getLocale();
        PagedMenu menu = new PagedMenu(3, Locales.getString(player, ParkourOption.LEADERBOARDS.getPath() + ".name", false));

        List<MenuItem> items = new ArrayList<>();

        Item base = Locales.getItem(player, ParkourOption.LEADERBOARDS.getPath() + ".head");

        Map<UUID, Score> sorted = sort.sort(leaderboard.getScores());

        for (UUID uuid : sorted.keySet()) {
            int rank = items.size() + 1;
            Score score = sorted.get(uuid);

            if (score == null) {
                continue;
            }

            Item item = base.clone()
                    .material(Material.PLAYER_HEAD)
                    .modifyName(name -> name.replace("%r", Integer.toString(rank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()))
                    .modifyLore(line -> line.replace("%r", Integer.toString(rank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()));

            // Player head gathering
            ItemStack stack = item.build();
            stack.setType(Material.PLAYER_HEAD);

            // if there are more than 36 players, don't show the heads to avoid server crashing
            // and bedrock has no player skull support
            if (rank <= 36 && !Util.isBedrockPlayer(player)) {
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
                menu.item(21, item.clone());
                item.glowing();
            }

            items.add(item);
        }

        // get next sorting type
        Sort next = switch (sort) {
            case SCORE -> Sort.TIME;
            case TIME -> Sort.DIFFICULTY;
            default -> Sort.SCORE;
        };

        List<String> values = Locales.getStringList(locale, ParkourOption.LEADERBOARDS.getPath() + ".sort.values", false);

        String name = switch (next) {
            case SCORE -> values.get(0);
            case TIME -> values.get(1);
            case DIFFICULTY -> values.get(2);
        };

        menu
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> menu.page(1)))

                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> menu.page(-1)))

                .item(22, Locales.getItem(player, ParkourOption.LEADERBOARDS.getPath() + ".sort", name.toLowerCase())
                        .click(event -> open(player, gamemode, next)))

                .item(23, Locales.getItem(player, "other.close")
                        .click(event -> Menus.COMMUNITY.open(event.getPlayer())))

                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE)
                .open(player);
    }

    public enum Sort {

        SCORE {
            @Override
            Map<UUID, Score> sort(Map<UUID, Score> scores) {
                return scores; // already sorted
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
                            String first = o1.getValue().difficulty();
                            String second = o2.getValue().difficulty();

                            double one = Double.parseDouble(first.equals("?") ? "1.0" : first);
                            double two = Double.parseDouble(second.equals("?") ? "1.0" : second);

                            return (int) (100 * (two - one)); // reverse natural order (higher == better)
                        }) // reverse natural order
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
            }
        };

        abstract Map<UUID, Score> sort(Map<UUID, Score> scores);
    }
}