package dev.efnilite.witp;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.PagedMenu;
import dev.efnilite.fycore.inventory.animation.RandomAnimation;
import dev.efnilite.fycore.inventory.animation.SnakeSingleAnimation;
import dev.efnilite.fycore.inventory.animation.WaveEastAnimation;
import dev.efnilite.fycore.inventory.animation.WaveWestAnimation;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.inventory.item.MenuItem;
import dev.efnilite.fycore.inventory.item.SliderItem;
import dev.efnilite.fycore.inventory.item.TimedItem;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.SkullSetter;
import dev.efnilite.fycore.util.Task;
import dev.efnilite.witp.api.Gamemode;
import dev.efnilite.witp.api.StyleType;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.player.data.Highscore;
import dev.efnilite.witp.util.Unicodes;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Handles all menu-related activities
 */
public class ParkourMenu {

    /**
     * Opens the gamemode menu
     *
     * @param   user
     *          The ParkourUser instance
     */
    public static void openGamemodeMenu(ParkourUser user) {
        WITP.getRegistry().close(); // prevent new registrations once a player has opened the gm menu

        Configuration config = WITP.getConfiguration();
        PagedMenu gamemode = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.locale + ".options.gamemode.name")));

        List<MenuItem> items = new ArrayList<>();
        for (Gamemode gm : WITP.getRegistry().getGamemodes()) {
            Item item = gm.getItem(user.locale);
            items.add(new Item(item.getMaterial(), item.getName())
                    .click((menu, event) -> gm.handleItemClick(user.getPlayer(), user, menu)));
        }

        gamemode
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click((menu, event) -> gamemode.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click((menu, event) -> gamemode.page(-1)))

                .item(31, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> user.getPlayer().closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(user.getPlayer());
    }
    
    /**
     * Shows the leaderboard menu
     *
     * @param   user
     *          The user, but can be null
     *
     * @param   player
     *          The player
     */
    public static void openLeaderboardMenu(@Nullable ParkourUser user, Player player) {
        ParkourUser.initHighScores(); // make sure scores are enabled

        // sort high scores by value
        HashMap<UUID, Integer> sorted = Util.sortByValue(ParkourUser.highScores);
        ParkourUser.highScores = sorted;
        List<UUID> uuids = new ArrayList<>(sorted.keySet());

        // init vars
        String locale = user == null ? Option.DEFAULT_LANG.get() : user.locale;
        Configuration config = WITP.getConfiguration();
        PagedMenu leaderboard = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + locale + ".options.leaderboard.name")));
        List<MenuItem> items = new ArrayList<>();

        int rank = 1;
        Item item = config.getFromItemData(locale, "options.leaderboard-head");
        for (UUID uuid : uuids) {
            Highscore highscore = ParkourUser.getHighscore(uuid);
            if (highscore == null) {
                continue;
            }

            int finalRank = rank;
            item = item.clone()
                    .material(Material.PLAYER_HEAD)
                    .modifyName(name -> name.replace("%r", Integer.toString(finalRank))
                            .replace("%s",  Integer.toString(ParkourUser.getHighestScore(uuid)))
                            .replace("%p", highscore.name)
                            .replace("%t", highscore.time)
                            .replace("%d", highscore.diff))
                    .modifyLore(line -> line.replace("%r", Integer.toString(finalRank))
                            .replace("%s",  Integer.toString(ParkourUser.getHighestScore(uuid)))
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
                        .click((menu, event) -> leaderboard.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click((menu, event) -> leaderboard.page(-1)))

                .item(32, config.getFromItemData(locale, "general.close")
                        .click((menu, event) -> player.closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(player);
    }

    /**
     * Shows the main menu to a valid ParkourPlayer instance
     *
     * @param   user
     *          The ParkourPlayer
     *
     * @param   disabledOptions
     *          An array of disabled options
     */
    public static void openMainMenu(ParkourPlayer user, ParkourOption... disabledOptions)  {
        Player player = user.getPlayer();
        Configuration config = WITP.getConfiguration();

        Menu main = new Menu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.locale + ".general.menu.name")));

        // ---------- top row ----------

        if (checkOptions(player, ParkourOption.STYLES, disabledOptions)) {
            main.item(0, config.getFromItemData(user.locale, "options." + ParkourOption.STYLES.getName(), user.style)
                    .click((menu, event) -> {
                            if (WITP.getRegistry().getStyleTypes().size() == 1) {
                                openSingleStyleMenu(user, WITP.getRegistry().getStyleTypes().get(0), disabledOptions);
                            }
                    }));
        }

        if (checkOptions(player, ParkourOption.LEADS, disabledOptions)) {
            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem()
                    .initial(leads.indexOf(user.blockLead)); // initial value of the player

            Item displayItem = config.getFromItemData(user.locale, "options." + ParkourOption.LEADS.getName());
            int slot = 0;
            for (int value : leads) {
                item.add(slot, displayItem.clone()
                                .amount(value)
                                .modifyLore(line -> line.replace("%s", Integer.toString(value))),
                        (menu, event) -> user.blockLead = value);
                slot++;
            }

            main.item(1, item);
        }

        if (checkOptions(player, ParkourOption.SCHEMATICS, disabledOptions)) {
            main.item(2, config.getFromItemData(user.locale, "options." + ParkourOption.SCHEMATICS.getName())
                    .click((menu, event) -> openSchematicMenu(user, disabledOptions)));
        }

        if (checkOptions(player, ParkourOption.TIME, disabledOptions)) {
            // Tick times start at 6:00 and total is 24,000.
            // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
            List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.TIME.getName());

            main.item(3, new SliderItem()
                    .initial(times.indexOf(user.time))
                    .add(0, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 AM" : "00:00")),
                            (menu, event) -> {
                                    user.time = 0;
                                    player.setPlayerTime(18000, false); // 00:00
                            })
                    .add(1, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 AM" : "6:00")),
                            (menu, event) -> {
                                user.time = 6000;
                                player.setPlayerTime(0, false); // 00:00
                            })
                    .add(2, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 PM" : "12:00")),
                            (menu, event) -> {
                                user.time = 12000;
                                player.setPlayerTime(6000, false); // 12:00
                            })
                    .add(3, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 PM" : "18:00")),
                            (menu, event) -> {
                                user.time = 18000;
                                player.setPlayerTime(12000, false); // 18:00
                            }));
        }

        // ---------- bottom row ----------

        if (checkOptions(player, ParkourOption.SHOW_SCOREBOARD, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SHOW_SCOREBOARD.getName());

            main.item(9, new SliderItem()
                    .initial(user.showScoreboard ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            (menu, event) -> {
                                    user.showScoreboard = true;
                                    user.setBoard(new FastBoard(player));
                                    user.updateScoreboard();
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            (menu, event) -> {
                                    user.showScoreboard = false;
                                    if (user.getBoard() != null && !user.getBoard().isDeleted()) {
                                        user.getBoard().delete();
                                    }
                            }));
        }

        if (checkOptions(player, ParkourOption.SHOW_FALL_MESSAGE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SHOW_FALL_MESSAGE.getName());

            main.item(10, new SliderItem()
                    .initial(user.showFallMessage ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            (menu, event) -> user.showFallMessage = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            (menu, event) -> user.showFallMessage = false));
        }

        if (checkOptions(player, ParkourOption.PARTICLES_AND_SOUND, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.PARTICLES_AND_SOUND.getName());

            main.item(11, new SliderItem()
                    .initial(user.useParticlesAndSound ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            (menu, event) -> user.useParticlesAndSound = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            (menu, event) -> user.useParticlesAndSound = false));
        }

        if (checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SPECIAL_BLOCKS.getName());

            main.item(12, new SliderItem()
                    .initial(user.useSpecialBlocks ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            (menu, event) -> {
                                if (allowSettingChange(user, menu, event)) {
                                    user.useSpecialBlocks = true;
                                }
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            (menu, event) -> {
                                if (allowSettingChange(user, menu, event)) {
                                    user.useSpecialBlocks = false;
                                }
                            }));
        }

        if (checkOptions(player, ParkourOption.SCORE_DIFFICULTY, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SCORE_DIFFICULTY.getName());

            main.item(13, new SliderItem()
                    .initial(user.useScoreDifficulty ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            (menu, event) -> {
                                if (allowSettingChange(user, menu, event)) {
                                    user.useScoreDifficulty = true;
                                }
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            (menu, event) -> {
                                if (allowSettingChange(user, menu, event)) {
                                    user.useScoreDifficulty = false;
                                }
                            }));
        }

        if (checkOptions(player, ParkourOption.GAMEMODE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.GAMEMODE.getName());

            main.item(19, item.click((menu, event) -> openGamemodeMenu(user)));
        }

        if (checkOptions(player, ParkourOption.LEADERBOARD, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.LEADERBOARD.getName())
                    .modifyLore(line -> // #%n (%s)
                            line.replace("%s", "#" + ParkourUser.getRank(user.getUUID()) + " (" +
                                    user.highScore.toString()) + ")");

            main.item(20, item.click((menu, event) -> {
                player.closeInventory();
                openLeaderboardMenu(user, player);
            }));
        }

        if (checkOptions(player, ParkourOption.LANGUAGE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.LANGUAGE.getName())
                    .modifyLore(line ->
                            line.replace("%s", user.locale));

            main.item(21, item.click((menu, event) -> openLangMenu(user, disabledOptions)));
        }


        // opens the menu
        main
                .distributeRowEvenly(0, 1, 2, 3)

                .item(28, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> user.getPlayer().closeInventory()))

                .item(29, config.getFromItemData(user.locale, "general.quit")
                        .click((menu, event) -> {
                            try {
                                ParkourPlayer.unregister(user, true, true, true);
                            } catch (IOException | InvalidStatementException ex) {
                                Logging.stack("Error while unregistering player", "Please report this error to the developer!", ex);
                            }
                        }))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation())
                .open(player);
    }

    /**
     * Opens the language menu
     *
     * @param   user
     *          The ParkourPlayer instance
     *
     * @param   disabledOptions
     *          Options which are disabled
     */
    public static void openLangMenu(ParkourPlayer user, ParkourOption... disabledOptions) {
        Configuration config = WITP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.locale + ".options.language.name")));

        List<MenuItem> items = new ArrayList<>();
        for (String lang : Option.LANGUAGES.get()) {
            Item item = new Item(Material.PAPER, "<#238681><bold>" + lang);

            items.add(item
                    .glowing(user.locale.equals(lang))
                    .click((menu, event) -> {
                        user.locale = lang;
                        user.lang = lang;
                        menu.update();
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click((menu, event) -> style.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click((menu, event) -> style.page(-1)))

                .item(31, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new WaveWestAnimation())
                .open(user.getPlayer());
    }

    /**
     * Opens the style menu for a specific style type
     *
     * @param   user
     *          The ParkourPlayer instance
     *
     * @param   styleType
     *          The style type
     *
     * @param   disabledOptions
     *          Disabled options
     */
    public static void openSingleStyleMenu(ParkourPlayer user, StyleType styleType, ParkourOption... disabledOptions) {
        Configuration config = WITP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.locale + ".options.styles.name")));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            Material material = Util.getRandom(styleType.styles.get(name));
            Item item = new Item(material, "<#238681><bold>" + name);

            items.add(item
                    .glowing(user.style.equals(name))
                    .click((menu, event) -> {
                        user.style = name;
                        menu.update();
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click((menu, event) -> style.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click((menu, event) -> style.page(-1)))

                .item(31, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(user.getPlayer());
    }

    /**
     * Opens the schematic customization menu
     *
     * @param   user
     *          The ParkourPlayer instance
     *
     * @param   disabledOptions
     *          Options which are disabled
     */
    public static void openSchematicMenu(ParkourPlayer user, ParkourOption... disabledOptions) {
        Configuration config = WITP.getConfiguration();

        // init menu
        Menu schematics = new Menu(3, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.locale + ".options.schematics.name")));

        List<Double> difficulties = Arrays.asList(0.2, 0.4, 0.6, 0.8);
        List<String> values = config.getStringList("items", "locale." + user.locale + ".options.schematic-difficulty.values");

        Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SCHEMATIC_DIFFICULTY.getName());

        schematics.item(10, new SliderItem()
                .initial(difficulties.indexOf(user.schematicDifficulty))
                .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<#0DCB07>" + values.get(0))),
                        (menu, event) -> {
                            if (allowSettingChange(user, menu, event)) {
                                user.schematicDifficulty = 0.2;
                            }
                        })
                .add(1, item.clone().material(Material.YELLOW_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<yellow>" + values.get(1))),
                        (menu, event) -> {
                            if (allowSettingChange(user, menu, event)) {
                                user.schematicDifficulty = 0.4;
                            }
                        })
                .add(2, item.clone().material(Material.ORANGE_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<#FF6C17>" + values.get(2))),
                        (menu, event) -> {
                            if (allowSettingChange(user, menu, event)) {
                                user.schematicDifficulty = 0.6;
                            }
                        })
                .add(3, item.clone().material(Material.SKELETON_SKULL)
                                .modifyLore(line -> line.replace("%s", "<dark_red>" + values.get(3))),
                        (menu, event) -> {
                            if (allowSettingChange(user, menu, event)) {
                                user.schematicDifficulty = 0.8;
                            }
                        }));

        item = config.getFromItemData(user.locale, "options." + ParkourOption.USE_SCHEMATICS.getName());

        schematics
                .distributeRowEvenly(0, 1, 2)

                .item(9, new SliderItem()
                        .initial(user.useSchematic ? 0 : 1)
                        .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                (menu, event) -> {
                                    if (allowSettingChange(user, menu, event)) {
                                        user.useSchematic = true;
                                    }
                                })
                        .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                (menu, event) -> {
                                    if (allowSettingChange(user, menu, event)) {
                                        user.useSchematic = false;
                                    }
                                }))

                .item(26, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.CYAN_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(user.getPlayer());
    }

    private static boolean allowSettingChange(ParkourPlayer player, Menu menu, InventoryClickEvent event) {
        if (player.getGenerator().getScore() > 0) {
            new Task()
                    .delay(1)
                    .execute(() -> {
                        menu.item(event.getSlot(), new TimedItem(new Item(Material.BARRIER, "<dark_red><bold>You can't change this right now")
                                .lore("<gray>You have a score above 0,", "<gray>which means you can't change settings.")
                                .click((menu1, event1) -> {

                                }), menu, event, 5 * 20));
                        menu.updateItem(event.getSlot());
                    })
                    .run();
            return false;
        }
        return true;
    }

    // replaces true/false with a checkmark and cross
    private static String getBooleanSymbol(boolean value) {
        return value ? "<#0DCB07><bold>" + Unicodes.HEAVY_CHECK : "<red><bold>" + Unicodes.HEAVY_CROSS;
    }

    // check if option is allowed to be displayed
    private static boolean checkOptions(@NotNull Player player, @NotNull ParkourOption option, ParkourOption[] disabled) {
        boolean enabled = WITP.getConfiguration().getFile("items").getBoolean("items.options." + option.getName() + ".enabled");
        if (!enabled || Arrays.asList(disabled).contains(option)) {
            return false;
        } else {
            return !Option.PERMISSIONS.get() || player.hasPermission(option.getPermission());
        }
    }
}