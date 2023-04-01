package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.style.StyleType;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.MenuClickEvent;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.inventory.item.SliderItem;
import dev.efnilite.vilib.inventory.item.TimedItem;
import dev.efnilite.vilib.lib.fastboard.fastboard.FastBoard;
import dev.efnilite.vilib.util.Unicodes;
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
 * @author Efnilite
 * @since v3.0.0
 */
public class ParkourSettingsMenu extends DynamicMenu {

    public ParkourSettingsMenu(ParkourOption... disabled) {

        // ---------- top row ----------

        // styles
        registerMainItem(1, 0, (p, user) -> Locales.getItem(p, ParkourOption.STYLES.path + ".item", user instanceof ParkourPlayer player ? player.style : null).click(event -> {
            if (!(user instanceof ParkourPlayer player)) {
                return;
            }

            if (IP.getRegistry().getStyleTypes().size() == 1) {
                openSingleStyleMenu(player, IP.getRegistry().getStyleTypes().get(0));
            } else {
                openStylesMenu(player);
            }
        }), player -> checkOptions(player, ParkourOption.STYLES, disabled));

        // leads
        registerMainItem(1, 1, (p, user) -> {
            Item displayItem = Locales.getItem(p, ParkourOption.LEADS.path);

            if (!(user instanceof ParkourPlayer player)) {
                return displayItem;
            }

            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem().initial(leads.indexOf(player.blockLead)); // initial value of the player

            int slot = 0;
            for (int value : leads) {
                item.add(slot, displayItem.clone().amount(value).modifyLore(line -> line.replace("%s", Integer.toString(value))), event2 -> {
                    player.blockLead = value;

                    player.updateGeneratorSettings();
                    return true;
                });
                slot++;
            }
            return item;
        }, player -> checkOptions(player, ParkourOption.LEADS, disabled));

        // schematics
        registerMainItem(1, 9, (p, user) -> Locales.getItem(p, ParkourOption.SCHEMATIC.path + ".item").click(event -> {
            if (!(user instanceof ParkourPlayer player)) {
                return;
            }

            openSchematicMenu(player, disabled);
        }), player -> checkOptions(player, ParkourOption.SCHEMATIC, disabled));

        // time
        registerMainItem(1, 10, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.TIME.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            // Tick times start at 6:00 and total is 24.000.
            // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
            List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

            return new SliderItem().initial(times.indexOf(player.selectedTime)).add(0, item.clone().modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 AM" : "00:00")), event -> {
                player.selectedTime = 0;

                player.updateGeneratorSettings();
                return true;
            }).add(1, item.clone().modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 AM" : "6:00")), event -> {
                player.selectedTime = 6000;

                player.updateGeneratorSettings();
                return true;
            }).add(2, item.clone().modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 PM" : "12:00")), event -> {
                player.selectedTime = 12000;

                player.updateGeneratorSettings();
                return true;
            }).add(3, item.clone().modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 PM" : "18:00")), event -> {
                player.selectedTime = 18000;

                player.updateGeneratorSettings();
                return true;
            });
        }, player -> checkOptions(player, ParkourOption.TIME, disabled));

        // ---------- bottom row ----------

        // show scoreboard
        registerMainItem(2, 0, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SCOREBOARD.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem().initial(player.showScoreboard ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                player.showScoreboard = true;
                player.board = new FastBoard(p);

                player.updateGeneratorSettings();
                return true;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                player.showScoreboard = false;
                if (player.board != null && !player.board.isDeleted()) {
                    player.board.delete();
                }

                player.updateGeneratorSettings();
                return true;
            });
        }, player -> checkOptions(player, ParkourOption.SCOREBOARD, disabled) && Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD)));

        // show fall message
        registerMainItem(2, 1, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.FALL_MESSAGE.path + ".item");

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem().initial(player.showFallMessage ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                player.showFallMessage = true;

                player.updateGeneratorSettings();
                return true;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                player.showFallMessage = false;

                player.updateGeneratorSettings();
                return true;
            });
        }, player -> checkOptions(player, ParkourOption.FALL_MESSAGE, disabled));

        // show sound
        registerMainItem(2, 2, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.PARTICLES.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem().initial(player.particles ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                player.particles = true;

                player.updateGeneratorSettings();
                return true;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                player.particles = false;

                player.updateGeneratorSettings();
                return true;
            });
        }, player -> checkOptions(player, ParkourOption.PARTICLES, disabled));

        // show sound
        registerMainItem(2, 3, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SOUND.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem().initial(player.sound ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                player.sound = true;

                player.updateGeneratorSettings();
                return true;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                player.sound = false;

                player.updateGeneratorSettings();
                return true;
            });
        }, player -> checkOptions(player, ParkourOption.SOUND, disabled));

        // show special blocks
        registerMainItem(2, 4, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SPECIAL_BLOCKS.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem().initial(player.useSpecialBlocks ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                if (allowSettingChange(player, event)) {
                    player.useSpecialBlocks = true;

                    player.updateGeneratorSettings();
                    return true;
                }
                return false;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                if (allowSettingChange(player, event)) {
                    player.useSpecialBlocks = false;

                    player.updateGeneratorSettings();
                    return true;
                }
                return false;
            });
        }, player -> checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabled));

        // Always allow closing of the menu
        registerMainItem(3, 10, (player, user) -> Locales.getItem(player, "other.close").click(event -> Menus.SETTINGS.open(event.getPlayer())), player -> true);
    }

    /**
     * Shows the main menu to a valid ParkourPlayer instance
     *
     * @param user The ParkourPlayer
     */
    public void open(ParkourPlayer user) {
        Player player = user.player;

        display(player, new Menu(4, Locales.getString(player, "settings.name"))
                .distributeRowEvenly(0, 1, 2, 3)
                .item(27, Locales.getItem(player, "other.close").click(event -> Menus.SETTINGS.open(event.getPlayer())))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE));
    }

    public void openStylesMenu(ParkourPlayer user) {
        // init menu
        Menu menu = new Menu(3, Locales.getString(user.locale, ParkourOption.STYLES.path + ".name"));

        int slot = 9;
        for (StyleType type : IP.getRegistry().getStyleTypes()) {
            Item item = type.getItem(user.locale);

            menu.item(slot, item.click(event -> openSingleStyleMenu(user, type)));
            slot++;
        }

        menu.distributeRowEvenly(1)
                .item(22, Locales.getItem(user.locale, "other.close").click(event -> open(user)))
                .fillBackground(Util.isBedrockPlayer(user.player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE).open(user.player);
    }

    /**
     * Opens the style menu for a specific style type
     *
     * @param user      The ParkourPlayer instance
     * @param styleType The style type
     */
    public void openSingleStyleMenu(ParkourPlayer user, StyleType styleType) {
        // init menu
        PagedMenu style = new PagedMenu(3, Locales.getString(user.locale, ParkourOption.STYLES.path + ".name"));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            String perm = ParkourOption.STYLES.permission + "." + name.toLowerCase();
            if (Option.PERMISSIONS_STYLES && !user.player.hasPermission(perm.replace(" ", "."))) {
                continue;
            }
            // todo add enchantment on select

            Item item = Locales.getItem(user.player, ParkourOption.STYLES.path + ".style_item", name).material(Colls.random(styleType.styles.get(name)));

            items.add(item.glowing(user.style.equals(name)).click(event -> {
                user.style = name;

                user.updateGeneratorSettings();

                open(user);
            }));
        }

        style.displayRows(0, 1).addToDisplay(items)

                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> style.page(1)))

                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> style.page(-1)))

                .item(22, Locales.getItem(user.locale, "other.close").click(event -> open(user)))

                .fillBackground(Util.isBedrockPlayer(user.player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE).open(user.player);
    }

    /**
     * Opens the schematic customization menu
     *
     * @param user The ParkourPlayer instance
     */
    public void openSchematicMenu(ParkourPlayer user, ParkourOption[] disabled) {
        // init menu
        Menu schematics = new Menu(3, Locales.getString(user.locale, ParkourOption.SCHEMATIC.path + ".name"));

        List<Double> difficulties = Arrays.asList(0.25, 0.5, 0.75, 1.0);
        List<String> values = Locales.getStringList(user.locale, ParkourOption.SCHEMATIC_DIFFICULTY.path + ".values");

        Item item = Locales.getItem(user.locale, ParkourOption.SCHEMATIC_DIFFICULTY.path);

        if (checkOptions(user.player, ParkourOption.SCHEMATIC_DIFFICULTY, disabled)) {
            schematics.item(10, new SliderItem().initial(difficulties.indexOf(user.schematicDifficulty)).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyLore(line -> line.replace("%s", "<#0DCB07>" + values.get(0))), event -> {
                if (allowSettingChange(user, event)) {
                    user.schematicDifficulty = 0.25;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }).add(1, item.clone().material(Material.YELLOW_STAINED_GLASS_PANE).modifyLore(line -> line.replace("%s", "<yellow>" + values.get(1))), event -> {
                if (allowSettingChange(user, event)) {
                    user.schematicDifficulty = 0.5;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }).add(2, item.clone().material(Material.ORANGE_STAINED_GLASS_PANE).modifyLore(line -> line.replace("%s", "<#FF6C17>" + values.get(2))), event -> {
                if (allowSettingChange(user, event)) {
                    user.schematicDifficulty = 0.75;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }).add(3, item.clone().material(Material.SKELETON_SKULL).modifyLore(line -> line.replace("%s", "<dark_red>" + values.get(3))), event -> {
                if (allowSettingChange(user, event)) {
                    user.schematicDifficulty = 1.0;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }));
        }

        item = Locales.getItem(user.locale, ParkourOption.USE_SCHEMATICS.path);

        if (checkOptions(user.player, ParkourOption.USE_SCHEMATICS, disabled)) {
            schematics.item(9, new SliderItem().initial(user.useSchematic ? 0 : 1).add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE).modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))), event -> {
                if (allowSettingChange(user, event)) {
                    user.useSchematic = true;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }).add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE).modifyName(name -> "<red><bold>" + ChatColor.stripColor(name)).modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))), event -> {
                if (allowSettingChange(user, event)) {
                    user.useSchematic = false;

                    user.updateGeneratorSettings();
                    return true;
                }
                return false;
            }));
        }

        schematics.distributeRowEvenly(0, 1, 2)

                .item(26, Locales.getItem(user.locale, "other.close").click(event -> open(user)))

                .fillBackground(Util.isBedrockPlayer(user.player) ? Material.AIR : Material.CYAN_STAINED_GLASS_PANE).open(user.player);
    }

    // If a player has a score above 0, disable options which change difficulty to keep leaderboards fair
    private boolean allowSettingChange(ParkourPlayer player, MenuClickEvent event) {
        if (player.generator.score > 0) {
            event.getMenu().item(event.getSlot(), new TimedItem(Locales.getItem(player.locale, "settings.parkour_settings.items.no_change").click((event1) -> {

            }), event).stay(5 * 20));
            event.getMenu().updateItem(event.getSlot());
            return false;
        }
        return true;
    }

    // replaces true/false with a checkmark and cross
    private String getBooleanSymbol(ParkourUser user, boolean value) {
        return value ? Locales.getString(user.player, "settings.parkour_settings.enabled") : Locales.getString(user.player, "settings.parkour_settings.disabled");
    }

    // check if option is allowed to be displayed
    private boolean checkOptions(@NotNull Player player, @NotNull ParkourOption option, ParkourOption[] disabled) {
        return !Arrays.asList(disabled).contains(option) && option.mayPerform(player);
    }
}