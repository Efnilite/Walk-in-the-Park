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
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.api.StyleType;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Unicodes;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParkourMenu {

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
                                .modifyLore(line -> line.replaceAll("%[a-z]", Integer.toString(value))),
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
                                            line.replaceAll("%[a-z]", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 AM" : "00:00")),
                            (menu, event) -> {
                                    user.time = 0;
                                    player.setPlayerTime(18000, false); // 00:00
                            })
                    .add(1, item.clone()
                                    .modifyLore(line ->
                                            line.replaceAll("%[a-z]", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 AM" : "6:00")),
                            (menu, event) -> {
                                user.time = 6000;
                                player.setPlayerTime(0, false); // 00:00
                            })
                    .add(2, item.clone()
                                    .modifyLore(line ->
                                            line.replaceAll("%[a-z]", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 PM" : "12:00")),
                            (menu, event) -> {
                                user.time = 12000;
                                player.setPlayerTime(6000, false); // 12:00
                            })
                    .add(3, item.clone()
                                    .modifyLore(line ->
                                            line.replaceAll("%[a-z]", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 PM" : "18:00")),
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
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                            (menu, event) -> {
                                    user.showScoreboard = true;
                                    user.setBoard(new FastBoard(player));
                                    user.updateScoreboard();
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
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
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                            (menu, event) -> user.showFallMessage = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
                            (menu, event) -> user.showFallMessage = false));
        }

        if (checkOptions(player, ParkourOption.PARTICLES_AND_SOUND, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.PARTICLES_AND_SOUND.getName());

            main.item(11, new SliderItem()
                    .initial(user.useParticlesAndSound ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                            (menu, event) -> user.useParticlesAndSound = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
                            (menu, event) -> user.useParticlesAndSound = false));
        }

        if (checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SPECIAL_BLOCKS.getName());

            main.item(12, new SliderItem()
                    .initial(user.useSpecialBlocks ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                            (menu, event) -> user.useSpecialBlocks = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
                            (menu, event) -> user.useSpecialBlocks = false));
        }

        if (checkOptions(player, ParkourOption.SCORE_DIFFICULTY, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SCORE_DIFFICULTY.getName());

            main.item(13, new SliderItem()
                    .initial(user.useScoreDifficulty ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                            (menu, event) -> user.useScoreDifficulty = true)
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
                            (menu, event) -> user.useScoreDifficulty = false));
        }

        if (checkOptions(player, ParkourOption.GAMEMODE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.GAMEMODE.getName());

            main.item(19, item.click((menu, event) -> user.gamemode()));
        }

        if (checkOptions(player, ParkourOption.LEADERBOARD, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.LEADERBOARD.getName())
                    .modifyLore(line ->
                            line.replaceAll("%s", user.getTranslated("your-rank",
                                    Integer.toString(ParkourUser.getRank(user.getUUID())),
                                    user.highScore.toString())));

            main.item(20, item.click((menu, event) -> {
                    ParkourUser.leaderboard(user, user.getPlayer(), 0);
                    event.getWhoClicked().closeInventory();
            }));
        }

        if (checkOptions(player, ParkourOption.LANGUAGE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.LANGUAGE.getName());

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
                                        .modifyLore(line -> line.replaceAll("%s", "<#0DCB07>" + values.get(0))),
                                (menu, event) -> user.schematicDifficulty = 0.2)
                        .add(1, item.clone().material(Material.YELLOW_STAINED_GLASS_PANE)
                                        .modifyLore(line -> line.replaceAll("%s", "<yellow>" + values.get(1))),
                                (menu, event) -> user.schematicDifficulty = 0.4)
                        .add(2, item.clone().material(Material.ORANGE_STAINED_GLASS_PANE)
                                        .modifyLore(line -> line.replaceAll("%s", "<#FF6C17>" + values.get(2))),
                                (menu, event) -> user.schematicDifficulty = 0.6)
                        .add(3, item.clone().material(Material.SKELETON_SKULL)
                                        .modifyLore(line -> line.replaceAll("%s", "<dark_red>" + values.get(3))),
                                (menu, event) -> user.schematicDifficulty = 0.8));

        item = config.getFromItemData(user.locale, "options." + ParkourOption.USE_SCHEMATICS.getName());

        schematics
                .distributeRowEvenly(0, 1, 2)

                .item(9, new SliderItem()
                        .initial(user.useSchematic ? 0 : 1)
                        .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(true))),
                                (menu, event) -> user.useSchematic = true)
                        .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replaceAll("%[a-z]", getBooleanSymbol(false))),
                                (menu, event) -> user.useSchematic = false))

                .item(26, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.CYAN_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(user.getPlayer());
    }

    private static String getBooleanSymbol(boolean value) {
        return value ? "<#0DCB07><bold>" + Unicodes.HEAVY_CHECK : "<red><bold>" + Unicodes.HEAVY_CROSS;
    }

    //Checks all options
    private static boolean checkOptions(@NotNull Player player, @NotNull ParkourOption option, ParkourOption[] disabled) {
        boolean enabled = WITP.getConfiguration().getFile("items").getBoolean("items.options." + option.getName() + ".enabled");
        if (!enabled || Arrays.asList(disabled).contains(option)) {
            return false;
        } else {
            return !Option.PERMISSIONS.get() || player.hasPermission(option.getPermission());
        }
    }
}