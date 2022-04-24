package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.StyleType;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Configuration;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.MenuClickEvent;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.animation.SnakeSingleAnimation;
import dev.efnilite.vilib.inventory.animation.WaveEastAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.inventory.item.SliderItem;
import dev.efnilite.vilib.inventory.item.TimedItem;
import dev.efnilite.vilib.util.Unicodes;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all menu-related activities
 *
 * @since v3.0.0
 * @author Efnilite
 */
public class SettingsMenu {

    /**
     * Shows the main menu to a valid ParkourPlayer instance
     *
     * @param   user
     *          The ParkourPlayer
     *
     * @param   disabledOptions
     *          An array of disabled options
     */
    public static void open(ParkourPlayer user, ParkourOption... disabledOptions)  {
        Player player = user.getPlayer();
        Configuration config = IP.getConfiguration();

        Menu main = new Menu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".general.menu.name")));

        // ---------- top row ----------

        if (checkOptions(player, ParkourOption.STYLES, disabledOptions)) {
            main.item(0, config.getFromItemData(user.getLocale(), "options." + ParkourOption.STYLES.getName(), user.style)
                    .click(event -> {
                        if (IP.getRegistry().getStyleTypes().size() == 1) {
                            openSingleStyleMenu(user, IP.getRegistry().getStyleTypes().get(0), disabledOptions);
                        } else {
                            openStylesMenu(user, disabledOptions);
                        }
                    }));
        }

        if (checkOptions(player, ParkourOption.LEADS, disabledOptions)) {
            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem()
                    .initial(leads.indexOf(user.blockLead)); // initial value of the player

            Item displayItem = config.getFromItemData(user.getLocale(), "options." + ParkourOption.LEADS.getName());
            int slot = 0;
            for (int value : leads) {
                item.add(slot, displayItem.clone()
                                .amount(value)
                                .modifyLore(line -> line.replace("%s", Integer.toString(value))),
                        event -> {
                            user.blockLead = value;
                            return true;
                        });
                slot++;
            }

            main.item(1, item);
        }

        if (checkOptions(player, ParkourOption.SCHEMATICS, disabledOptions)) {
            main.item(2, config.getFromItemData(user.getLocale(), "options." + ParkourOption.SCHEMATICS.getName())
                    .click(event -> openSchematicMenu(user, disabledOptions)));
        }

        if (checkOptions(player, ParkourOption.TIME, disabledOptions)) {
            // Tick times start at 6:00 and total is 24,000.
            // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
            List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.TIME.getName());

            main.item(3, new SliderItem()
                    .initial(times.indexOf(user.selectedTime))
                    .add(0, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 AM" : "00:00")),
                            event -> {
                                user.selectedTime = 0;
                                user.updateVisualTime(user.selectedTime);
                                return true;
                            })
                    .add(1, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 AM" : "6:00")),
                            event -> {
                                user.selectedTime = 6000;
                                user.updateVisualTime(user.selectedTime);
                                return true;
                            })
                    .add(2, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 PM" : "12:00")),
                            event -> {
                                user.selectedTime = 12000;
                                user.updateVisualTime(user.selectedTime);
                                return true;
                            })
                    .add(3, item.clone()
                                    .modifyLore(line ->
                                            line.replace("%s", Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 PM" : "18:00")),
                            event -> {
                                user.selectedTime = 18000;
                                user.updateVisualTime(user.selectedTime);
                                return true;
                            }));
        }

        // ---------- bottom row ----------

        if (checkOptions(player, ParkourOption.SHOW_SCOREBOARD, disabledOptions) && Option.SCOREBOARD.get()) {
            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.SHOW_SCOREBOARD.getName());

            main.item(9, new SliderItem()
                    .initial(user.showScoreboard ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                    user.showScoreboard = true;
                                    user.setBoard(new FastBoard(player));
                                    user.updateScoreboard();
                                    return true;
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            event -> {
                                    user.showScoreboard = false;
                                    if (user.getBoard() != null && !user.getBoard().isDeleted()) {
                                        user.getBoard().delete();
                                    }
                                    return true;
                            }));
        }

        if (checkOptions(player, ParkourOption.SHOW_FALL_MESSAGE, disabledOptions)) {
            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.SHOW_FALL_MESSAGE.getName());

            main.item(10, new SliderItem()
                    .initial(user.showFallMessage ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                user.showFallMessage = true;
                                return true;
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            event -> {
                                user.showFallMessage = false;
                                return true;
                            }));
        }

        if (checkOptions(player, ParkourOption.PARTICLES_AND_SOUND, disabledOptions)) {
            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.PARTICLES_AND_SOUND.getName());

            main.item(11, new SliderItem()
                    .initial(user.useParticlesAndSound ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                user.useParticlesAndSound = true;
                                return true;
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            event -> {
                                user.useParticlesAndSound = false;
                                return true;
                            }));
        }

        if (checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabledOptions)) {
            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.SPECIAL_BLOCKS.getName());

            main.item(12, new SliderItem()
                    .initial(user.useSpecialBlocks ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.useSpecialBlocks = true;
                                    return true;
                                }
                                return false;
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.useSpecialBlocks = false;
                                    return true;
                                }
                                return false;
                            }));
        }

        if (checkOptions(player, ParkourOption.SCORE_DIFFICULTY, disabledOptions)) {
            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.SCORE_DIFFICULTY.getName());

            main.item(13, new SliderItem()
                    .initial(user.useScoreDifficulty ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.useScoreDifficulty = true;
                                    return true;
                                }
                                return false;
                            })
                    .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.useScoreDifficulty = false;
                                    return true;
                                }
                                return false;
                            }));
        }

//        if (checkOptions(player, ParkourOption.GAMEMODE, disabledOptions)) {
//            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.GAMEMODE.getName());
//
//            main.item(19, item.click(event -> GamemodeMenu.open(user)));
//        }
//
//        if (checkOptions(player, ParkourOption.LEADERBOARD, disabledOptions)) {
//            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.LEADERBOARD.getName())
//                    .modifyLore(line -> // #%n (%s)
//                            line.replace("%s", "#" + ParkourUser.getRank(user.getUUID()) + " (" +
//                                    user.highScore.toString()) + ")");
//
//            main.item(20, item.click(event -> {
//                player.closeInventory();
//                LeaderboardMenu.open(player);
//            }));
//        }
//
//        if (checkOptions(player, ParkourOption.LANGUAGE, disabledOptions)) {
//            Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.LANGUAGE.getName())
//                    .modifyLore(line ->
//                            line.replace("%s", user.getLocale()));
//
//            main.item(21, item.click(event -> LangMenu.open(user, disabledOptions)));
//        }

        // opens the menu
        main
                .distributeRowEvenly(0, 1, 2, 3)

                .item(28, config.getFromItemData(user.getLocale(), "general.close")
                        .click(event -> user.getPlayer().closeInventory()))

                .item(29, config.getFromItemData(user.getLocale(), "general.quit")
                        .click(event -> ParkourPlayer.leave(user)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation())
                .open(player);
    }

    public static void openStylesMenu(ParkourPlayer user, ParkourOption... disabledOptions) {
        Configuration config = IP.getConfiguration();

        // init menu
        Menu menu = new Menu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.styles.name")));

        int slot = 9;
        for (StyleType type : IP.getRegistry().getStyleTypes()) {
            Item item = type.getItem(user.getLocale());

            menu.item(slot, item.click(event -> openSingleStyleMenu(user, type, disabledOptions)));
            slot++;
        }

        menu
                .distributeRowEvenly(1)

                .item(31, config.getFromItemData(user.getLocale(), "general.close")
                        .click(event -> open(user, disabledOptions)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
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
        Configuration config = IP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.styles.name")));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            String perm = ParkourOption.STYLES.getPermission() + "." + name.toLowerCase();
            if (Option.PERMISSIONS_STYLES.get() && !user.getPlayer().hasPermission(perm.replace(" ", "."))) {
                continue;
            }

            Material material = Util.getRandom(styleType.styles.get(name));
            Item item = new Item(material, "<#238681><bold>" + name); // todo add enchantment on select

            items.add(item
                    .glowing(user.style.equals(name))
                    .click(event -> {
                        user.style = name;
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
                        .click(event -> open(user, disabledOptions)))

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
        Configuration config = IP.getConfiguration();

        // init menu
        Menu schematics = new Menu(3, "<white>" +
                ChatColor.stripColor(config.getString("items", "locale." + user.getLocale() + ".options.schematics.name")));

        List<Double> difficulties = Arrays.asList(0.2, 0.4, 0.6, 0.8);
        List<String> values = config.getStringList("items", "locale." + user.getLocale() + ".options.schematic-difficulty.values");

        Item item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.SCHEMATIC_DIFFICULTY.getName());

        schematics.item(10, new SliderItem()
                .initial(difficulties.indexOf(user.schematicDifficulty))
                .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<#0DCB07>" + values.get(0))),
                        event -> {
                            if (allowSettingChange(user, event)) {
                                user.schematicDifficulty = 0.2;
                                return true;
                            }
                            return false;
                        })
                .add(1, item.clone().material(Material.YELLOW_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<yellow>" + values.get(1))),
                        event -> {
                            if (allowSettingChange(user, event)) {
                                user.schematicDifficulty = 0.4;
                                return true;
                            }
                            return false;
                        })
                .add(2, item.clone().material(Material.ORANGE_STAINED_GLASS_PANE)
                                .modifyLore(line -> line.replace("%s", "<#FF6C17>" + values.get(2))),
                        event -> {
                            if (allowSettingChange(user, event)) {
                                user.schematicDifficulty = 0.6;
                                return true;
                            }
                            return false;
                        })
                .add(3, item.clone().material(Material.SKELETON_SKULL)
                                .modifyLore(line -> line.replace("%s", "<dark_red>" + values.get(3))),
                        event -> {
                            if (allowSettingChange(user, event)) {
                                user.schematicDifficulty = 0.8;
                                return true;
                            }
                            return false;
                        }));

        item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.USE_SCHEMATICS.getName());

        schematics
                .distributeRowEvenly(0, 1, 2)

                .item(9, new SliderItem()
                        .initial(user.useSchematic ? 0 : 1)
                        .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                event -> {
                                    if (allowSettingChange(user, event)) {
                                        user.useSchematic = true;
                                        return true;
                                    }
                                    return false;
                                })
                        .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                event -> {
                                    if (allowSettingChange(user, event)) {
                                        user.useSchematic = false;
                                        return true;
                                    }
                                    return false;
                                }))

                .item(26, config.getFromItemData(user.getLocale(), "general.close")
                        .click(event -> open(user, disabledOptions)))

                .fillBackground(Material.CYAN_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(user.getPlayer());
    }

    // If a player has a score above 0, disable options which change difficulty to keep leaderboards fair
    private static boolean allowSettingChange(ParkourPlayer player, MenuClickEvent event) {
        if (player.getGenerator().getScore() > 0) {
            event.getMenu().item(event.getSlot(), new TimedItem(IP.getConfiguration().getFromItemData(player.getLocale(), "options.cant-change")
                    .click((event1) -> {

                    }), event).stay(5 * 20));
            event.getMenu().updateItem(event.getSlot());
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
        boolean enabled = IP.getConfiguration().getFile("items").getBoolean("items.options." + option.getName() + ".enabled");
        if (!enabled || Arrays.asList(disabled).contains(option)) {
            return false;
        } else {
            return !Option.PERMISSIONS.get() || player.hasPermission(option.getPermission());
        }
    }
}