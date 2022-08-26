package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.StyleType;
import dev.efnilite.ip.config.Configuration;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.MenuClickEvent;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
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
public class ParkourSettingsMenu extends DynamicMenu {

    public ParkourSettingsMenu(ParkourOption... disabled) {

        // ---------- top row ----------

        // styles
        registerMainItem(1, 0,
                (p, user) -> Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.STYLES.getName(),
                        user instanceof ParkourPlayer player ? player.style : null).click(event -> {
                    if (!(user instanceof ParkourPlayer player)) {
                        return;
                    }

                    if (IP.getRegistry().getStyleTypes().size() == 1) {
                        openSingleStyleMenu(player, IP.getRegistry().getStyleTypes().get(0));
                    } else {
                        openStylesMenu(player);
                    }}),
                player -> checkOptions(player, ParkourOption.STYLES, disabled));

        // leads
        registerMainItem(1, 1,
                (p, user) -> {
                    Item displayItem = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.LEADS.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return displayItem;
                    }

                    List<Integer> leads = Option.POSSIBLE_LEADS;

                    SliderItem item = new SliderItem()
                            .initial(leads.indexOf(player.blockLead)); // initial value of the player

                    int slot = 0;
                    for (int value : leads) {
                        item.add(slot, displayItem.clone()
                                        .amount(value)
                                        .modifyLore(line -> line.replace("%s", Integer.toString(value))),
                                event2 -> {
                                    player.blockLead = value;

                                    player.updateGeneratorSettings();
                                    return true;
                                });
                        slot++;
                    }
                    return item;
                },
                player -> checkOptions(player, ParkourOption.LEADS, disabled));

        // schematics
        registerMainItem(1, 9,
                (p, user) -> Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.SCHEMATICS.getName()).click(
                event -> {
                    if (!(user instanceof ParkourPlayer player)) {
                        return;
                    }

                    openSchematicMenu(player, disabled);
                }),
                player -> checkOptions(player, ParkourOption.SCHEMATICS, disabled));

        // time
        registerMainItem(1, 10,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.TIME.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    // Tick times start at 6:00 and total is 24.000.
                    // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
                    List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

                    return new SliderItem()
                            .initial(times.indexOf(player.selectedTime))
                            .add(0, item.clone()
                                            .modifyLore(line ->
                                                    line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 AM" : "00:00")),
                                    event -> {
                                        player.selectedTime = 0;
                                        player.updateVisualTime(player.selectedTime);

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(1, item.clone()
                                            .modifyLore(line ->
                                                    line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 AM" : "6:00")),
                                    event -> {
                                        player.selectedTime = 6000;
                                        player.updateVisualTime(player.selectedTime);

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(2, item.clone()
                                            .modifyLore(line ->
                                                    line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 PM" : "12:00")),
                                    event -> {
                                        player.selectedTime = 12000;
                                        player.updateVisualTime(player.selectedTime);

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(3, item.clone()
                                            .modifyLore(line ->
                                                    line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 PM" : "18:00")),
                                    event -> {
                                        player.selectedTime = 18000;
                                        player.updateVisualTime(player.selectedTime);

                                        player.updateGeneratorSettings();
                                        return true;
                                    });
                },
                player -> checkOptions(player, ParkourOption.TIME, disabled));

        // ---------- bottom row ----------

        // show scoreboard
        registerMainItem(2, 0,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.SHOW_SCOREBOARD.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    return new SliderItem()
                            .initial(player.showScoreboard ? 0 : 1)
                            .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                    event -> {
                                        player.showScoreboard = true;
                                        player.setBoard(new FastBoard(p));
                                        player.getGenerator().updateScoreboard();

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                    event -> {
                                        player.showScoreboard = false;
                                        if (player.board != null && !player.board.isDeleted()) {
                                            player.board.delete();
                                        }

                                        player.updateGeneratorSettings();
                                        return true;
                                    });
                },
                player -> checkOptions(player, ParkourOption.SHOW_SCOREBOARD, disabled) && Option.SCOREBOARD_ENABLED);

        // show fall message
        registerMainItem(2, 1,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.SHOW_FALL_MESSAGE.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    return new SliderItem()
                            .initial(player.showFallMessage ? 0 : 1)
                            .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                    event -> {
                                        player.showFallMessage = true;

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                    event -> {
                                        player.showFallMessage = false;

                                        player.updateGeneratorSettings();
                                        return true;
                                    });
                },
                player -> checkOptions(player, ParkourOption.SHOW_FALL_MESSAGE, disabled));

        // show particles and sound
        registerMainItem(2, 2,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.PARTICLES_AND_SOUND.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    return new SliderItem()
                            .initial(player.useParticlesAndSound ? 0 : 1)
                            .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                    event -> {
                                        player.useParticlesAndSound = true;

                                        player.updateGeneratorSettings();
                                        return true;
                                    })
                            .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                    event -> {
                                        player.useParticlesAndSound = false;

                                        player.updateGeneratorSettings();
                                        return true;
                                    });
                },
                player -> checkOptions(player, ParkourOption.PARTICLES_AND_SOUND, disabled));

        // show special blocks
        registerMainItem(2, 3,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.SPECIAL_BLOCKS.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    return new SliderItem()
                            .initial(player.useSpecialBlocks ? 0 : 1)
                            .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                    event -> {
                                        if (allowSettingChange(player, event)) {
                                            player.useSpecialBlocks = true;

                                            player.updateGeneratorSettings();
                                            return true;
                                        }
                                        return false;
                                    })
                            .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                    event -> {
                                        if (allowSettingChange(player, event)) {
                                            player.useSpecialBlocks = false;

                                            player.updateGeneratorSettings();
                                            return true;
                                        }
                                        return false;
                                    });
                },
                player -> checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabled));

        // show score difficulty
        registerMainItem(2, 4,
                (p, user) -> {
                    Item item = Locales.getItem(p, "settings.parkour_settings.items." + ParkourOption.SCORE_DIFFICULTY.getName());

                    if (!(user instanceof ParkourPlayer player)) {
                        return item;
                    }

                    return new SliderItem()
                            .initial(player.useScoreDifficulty ? 0 : 1)
                            .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                                    event -> {
                                        if (allowSettingChange(player, event)) {
                                            player.useScoreDifficulty = true;

                                            player.updateGeneratorSettings();
                                            return true;
                                        }
                                        return false;
                                    })
                            .add(1, item.clone().material(Material.RED_STAINED_GLASS_PANE)
                                            .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                                            .modifyLore(line -> line.replace("%s", getBooleanSymbol(false))),
                                    event -> {
                                        if (allowSettingChange(player, event)) {
                                            player.useScoreDifficulty = false;

                                            player.updateGeneratorSettings();
                                            return true;
                                        }
                                        return false;
                                    });
                },
                player -> checkOptions(player, ParkourOption.SCORE_DIFFICULTY, disabled));

        // Always allow closing of the menu
        registerMainItem(3, 10,
                (player, user) -> Locales.getItem(player, "other.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    /**
     * Shows the main menu to a valid ParkourPlayer instance
     *
     * @param   user
     *          The ParkourPlayer
     */
    public void open(ParkourPlayer user)  {
        Player player = user.player;

        Menu menu = new Menu(4, Locales.getString(player, "settings.name"))
                .distributeRowEvenly(0, 1, 2, 3)
                .item(27, Locales.getItem(player, "other.close").click(
                        event -> Menus.SETTINGS.open(event.getPlayer())))
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation());

        display(player, menu);
    }

    public void openStylesMenu(ParkourPlayer user) {
        // init menu
        Menu menu = new Menu(4, Locales.getString(user.getLocale(), "settings.parkour_settings.items.styles.name"));

        int slot = 9;
        for (StyleType type : IP.getRegistry().getStyleTypes()) {
            Item item = type.getItem(user.getLocale());

            menu.item(slot, item.click(event -> openSingleStyleMenu(user, type)));
            slot++;
        }

        menu
                .distributeRowEvenly(1)

                .item(31, Locales.getItem(user.getLocale(), "other.close")
                        .click(event -> open(user)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .open(user.player);
    }

    /**
     * Opens the style menu for a specific style type
     *
     * @param   user
     *          The ParkourPlayer instance
     *
     * @param   styleType
     *          The style type
     */
    public void openSingleStyleMenu(ParkourPlayer user, StyleType styleType) {
        // init menu
        PagedMenu style = new PagedMenu(4, Locales.getString(user.getLocale(), "settings.parkour_settings.items.styles.name"));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            String perm = ParkourOption.STYLES.getPermission() + "." + name.toLowerCase();
            if (Option.PERMISSIONS_STYLES && !user.player.hasPermission(perm.replace(" ", "."))) {
                continue;
            }

            Material material = Util.getRandom(styleType.styles.get(name));
            Item item = new Item(material, "<#238681><bold>" + name); // todo add enchantment on select

            items.add(item
                    .glowing(user.style.equals(name))
                    .click(event -> {
                        user.style = name;

                        user.updateGeneratorSettings();

                        open(user);
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> style.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> style.page(-1)))

                .item(31, Locales.getItem(user.getLocale(), "other.close")
                        .click(event -> open(user)))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(user.player);
    }

    /**
     * Opens the schematic customization menu
     *
     * @param   user
     *          The ParkourPlayer instance
     *
     */
    public void openSchematicMenu(ParkourPlayer user, ParkourOption[] disabled) {
        Configuration config = IP.getConfiguration();

        // init menu
        Menu schematics = new Menu(3, Locales.getString(user.getLocale(), "settings.parkour_settings.items.schematic.name");

        List<Double> difficulties = Arrays.asList(0.2, 0.4, 0.6, 0.8);
        List<String> values = config.getStringList("items", "locale." + user.getLocale() + ".options.schematic-difficulty.values");

        Item item = Locales.getItem(user.getLocale(), "settings.parkour_settings.items." + ParkourOption.SCHEMATIC_DIFFICULTY.getName());

        if (checkOptions(user.player, ParkourOption.SCHEMATIC_DIFFICULTY, disabled)) {
            schematics.item(10, new SliderItem()
                    .initial(difficulties.indexOf(user.schematicDifficulty))
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyLore(line -> line.replace("%s", "<#0DCB07>" + values.get(0))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.schematicDifficulty = 0.2;

                                    user.updateGeneratorSettings();
                                    return true;
                                }
                                return false;
                            })
                    .add(1, item.clone().material(Material.YELLOW_STAINED_GLASS_PANE)
                                    .modifyLore(line -> line.replace("%s", "<yellow>" + values.get(1))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.schematicDifficulty = 0.4;

                                    user.updateGeneratorSettings();
                                    return true;
                                }
                                return false;
                            })
                    .add(2, item.clone().material(Material.ORANGE_STAINED_GLASS_PANE)
                                    .modifyLore(line -> line.replace("%s", "<#FF6C17>" + values.get(2))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.schematicDifficulty = 0.6;

                                    user.updateGeneratorSettings();
                                    return true;
                                }
                                return false;
                            })
                    .add(3, item.clone().material(Material.SKELETON_SKULL)
                                    .modifyLore(line -> line.replace("%s", "<dark_red>" + values.get(3))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.schematicDifficulty = 0.8;

                                    user.updateGeneratorSettings();
                                    return true;
                                }
                                return false;
                            }));
        }

        item = config.getFromItemData(user.getLocale(), "options." + ParkourOption.USE_SCHEMATICS.getName());

        if (checkOptions(user.player, ParkourOption.USE_SCHEMATICS, disabled)) {
            schematics.item(9, new SliderItem()
                    .initial(user.useSchematic ? 0 : 1)
                    .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(true))),
                            event -> {
                                if (allowSettingChange(user, event)) {
                                    user.useSchematic = true;

                                    user.updateGeneratorSettings();
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

                                    user.updateGeneratorSettings();
                                    return true;
                                }
                                return false;
                            }));
        }

        schematics
                .distributeRowEvenly(0, 1, 2)

                .item(26, Locales.getItem(user.getLocale(), "other.close")
                        .click(event -> open(user)))

                .fillBackground(Material.CYAN_STAINED_GLASS_PANE)
                .animation(new WaveEastAnimation())
                .open(user.player);
    }

    // If a player has a score above 0, disable options which change difficulty to keep leaderboards fair
    private boolean allowSettingChange(ParkourPlayer player, MenuClickEvent event) {
        if (player.getGenerator().getScore() > 0) {
            event.getMenu().item(event.getSlot(), new TimedItem(Locales.getItem(player.getLocale(), "settings.parkour_settings.items.no_change")
                    .click((event1) -> {

                    }), event).stay(5 * 20));
            event.getMenu().updateItem(event.getSlot());
            return false;
        }
        return true;
    }

    // replaces true/false with a checkmark and cross
    private String getBooleanSymbol(boolean value) {
        return value ? "<#0DCB07><bold>" + Unicodes.HEAVY_CHECK : "<red><bold>" + Unicodes.HEAVY_CROSS;
    }

    // check if option is allowed to be displayed
    private boolean checkOptions(@NotNull Player player, @NotNull ParkourOption option, ParkourOption[] disabled) {
        boolean enabled = Option.OPTIONS_ENABLED.getOrDefault(option, true);

        if (!enabled || Arrays.asList(disabled).contains(option)) {
            return false;
        } else {
            return !Option.PERMISSIONS || player.hasPermission(option.getPermission());
        }
    }
}