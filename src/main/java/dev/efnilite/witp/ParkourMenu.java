package dev.efnilite.witp;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.PagedMenu;
import dev.efnilite.fycore.inventory.animation.RandomAnimation;
import dev.efnilite.fycore.inventory.animation.SnakeSingleAnimation;
import dev.efnilite.fycore.inventory.animation.WaveEastAnimation;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.inventory.item.MenuItem;
import dev.efnilite.fycore.inventory.item.SliderItem;
import dev.efnilite.witp.api.StyleType;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.Unicodes;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParkourMenu {

    public static void openMainMenu(ParkourPlayer user, ParkourOption... disabledOptions)  {
        Player player = user.getPlayer();
        Configuration config = WITP.getConfiguration();

        Menu main = new Menu(4, config.getString("items", "locale." + user.locale + ".general.menu.name"));

        // ---------- top row ----------

        if (checkOptions(player, ParkourOption.STYLES, disabledOptions)) {
            main.item(0, config.getFromItemData(user.locale, "options." + ParkourOption.STYLES.getName(), user.style)
                    .click((menu, event) -> {
                            if (WITP.getRegistry().getStyleTypes().size() == 1) {
                                openSingleStyle(user, WITP.getRegistry().getStyleTypes().get(0), disabledOptions);
                            }
                    }));
        }

        if (checkOptions(player, ParkourOption.LEADS, disabledOptions)) {
            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem()
                    .initial(leads.indexOf(user.blockLead)); // initial value of the player

            for (int value : leads) {
                item.add(leads.indexOf(value),
                        config.getFromItemData(user.locale, "options." + ParkourOption.LEADS.getName(), Integer.toString(user.blockLead)),
                        (menu, event) -> user.blockLead = value);
            }

            main.item(1, item);
        }

        if (checkOptions(player, ParkourOption.TIME, disabledOptions)) {
            // Tick times start at 6:00 and total is 24,000.
            // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
            List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

            main.item(2, new SliderItem()
                    .initial(times.indexOf(user.time))
                    .add(0, new Item(Material.BLUE_STAINED_GLASS_PANE, "<#1666CF><bold>" + (Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 AM" : "00:00"))
                                    .lore("<gray>Your current time."),
                            (menu, event) -> {
                                    user.time = 0;
                                    player.setPlayerTime(18000, false); // 00:00
                            })
                    .add(1, new Item(Material.BLUE_STAINED_GLASS_PANE, "<#1666CF><bold>" + (Option.OPTIONS_TIME_FORMAT.get() == 12 ? "06:00 AM" : "06:00"))
                                    .lore("<gray>Your current time."),
                            (menu, event) -> {
                                user.time = 6000;
                                player.setPlayerTime(0, false); // 00:00
                            })
                    .add(2, new Item(Material.BLUE_STAINED_GLASS_PANE, "<#1666CF><bold>" + (Option.OPTIONS_TIME_FORMAT.get() == 12 ? "12:00 PM" : "12:00"))
                                    .lore("<gray>Your current time."),
                            (menu, event) -> {
                                user.time = 12000;
                                player.setPlayerTime(6000, false); // 12:00
                            })
                    .add(3, new Item(Material.BLUE_STAINED_GLASS_PANE, "<#1666CF><bold>" + (Option.OPTIONS_TIME_FORMAT.get() == 12 ? "6:00 PM" : "18:00"))
                                    .lore("<gray>Your current time."),
                            (menu, event) -> {
                                user.time = 18000;
                                player.setPlayerTime(12000, false); // 18:00
                            }));
        }

        // ---------- bottom row ----------

        if (checkOptions(player, ParkourOption.SHOW_SCOREBOARD, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SHOW_SCOREBOARD.getName(), getBooleanSymbol(user.showScoreboard));

            main.item(9, new SliderItem()
                    .initial(user.showScoreboard ? 0 : 1)
                    .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.showScoreboard = true)
                    .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.showScoreboard = false));
        }

        if (checkOptions(player, ParkourOption.SHOW_FALL_MESSAGE, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SHOW_FALL_MESSAGE.getName(), getBooleanSymbol(user.showFallMessage));

            main.item(10, new SliderItem()
                    .initial(user.showFallMessage ? 0 : 1)
                    .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.showFallMessage = true)
                    .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.showFallMessage = false));
        }

        if (checkOptions(player, ParkourOption.PARTICLES_AND_SOUND, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.PARTICLES_AND_SOUND.getName(), getBooleanSymbol(user.useParticlesAndSound));

            main.item(11, new SliderItem()
                    .initial(user.useParticlesAndSound ? 0 : 1)
                    .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useParticlesAndSound = true)
                    .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useParticlesAndSound = false));
        }

        if (checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SPECIAL_BLOCKS.getName(), getBooleanSymbol(user.useSpecialBlocks));

            main.item(12, new SliderItem()
                    .initial(user.useSpecialBlocks ? 0 : 1)
                    .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useSpecialBlocks = true)
                    .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useSpecialBlocks = false));
        }

        if (checkOptions(player, ParkourOption.SCORE_DIFFICULTY, disabledOptions)) {
            Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SCORE_DIFFICULTY.getName(), getBooleanSymbol(user.useScoreDifficulty));

            main.item(13, new SliderItem()
                    .initial(user.useScoreDifficulty ? 0 : 1)
                    .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useScoreDifficulty = true)
                    .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                            (menu, event) -> user.useScoreDifficulty = false));
        }

        // opens the menu
        main
                .distributeRowsEvenly()

                .item(28, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> user.getPlayer().closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation())
                .open(player);
    }

    public static void openSingleStyle(ParkourPlayer user, StyleType styleType, ParkourOption... disabledOptions) {
        Configuration config = WITP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, config.getString("items", "locale." + user.locale + ".general.menu.name"));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            Material item = Util.getRandom(styleType.styles.get(name));

            items.add(new Item(item, "<gradient:#238681>" + name + "</gradient:#1DC9C1>")
                    .glowing(user.style.equals(name))
                    .click((menu, event) -> user.style = name));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(27, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // next page
                        .click((menu, event) -> style.page(1)))

                .prevPage(35, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // previous page
                        .click((menu, event) -> style.page(-1)))

                .item(31, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(user.getPlayer());
    }

    public static void openSchematicMenu(ParkourPlayer user, ParkourOption... disabledOptions) {
        Configuration config = WITP.getConfiguration();

        // init menu
        Menu schematics = new Menu(3, "Schematics"); // todo

        Item item = config.getFromItemData(user.locale, "options." + ParkourOption.SCHEMATICS.getName(), getBooleanSymbol(user.useSpecialBlocks));

        schematics
                .item(13, new SliderItem()
                        .initial(user.useSchematic ? 0 : 1)
                        .add(0, new Item(Material.GREEN_STAINED_GLASS_PANE, item.getName()),
                                (menu, event) -> user.useSchematic = true)
                        .add(1, new Item(Material.RED_STAINED_GLASS_PANE, item.getName()),
                                (menu, event) -> user.useSchematic = false));

        schematics
                .distributeRowsEvenly()

                .item(26, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.RED_STAINED_GLASS_PANE)
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
            return Option.PERMISSIONS.get() && player.hasPermission(option.getPermission());
        }
    }
}