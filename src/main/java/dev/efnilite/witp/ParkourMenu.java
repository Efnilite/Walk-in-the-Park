package dev.efnilite.witp;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.PagedMenu;
import dev.efnilite.fycore.inventory.animation.RandomAnimation;
import dev.efnilite.fycore.inventory.animation.SnakeSingleAnimation;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.inventory.item.MenuItem;
import dev.efnilite.fycore.inventory.item.SliderItem;
import dev.efnilite.witp.api.StyleType;
import dev.efnilite.witp.player.ParkourPlayer;
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
        // init generic values
        Player player = user.getPlayer();
        String locale = user.locale;
        Configuration config = WITP.getConfiguration();

        Menu main = new Menu(4, config.getString("items", "locale." + locale + ".general.menu.name"));

        if (checkOptions(player, ParkourOption.STYLES, disabledOptions)) {
            main.item(0, config.getFromItemData(locale, "options.styles", user.style)
                    .click((menu, event) -> {
                            if (WITP.getRegistry().getStyleTypes().size() == 1) {
                                openSingleStyle(user, WITP.getRegistry().getStyleTypes().get(0), disabledOptions);
                            }
                    }));
        }

        // option for lead
        if (checkOptions(player, ParkourOption.LEAD, disabledOptions)) {
            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem()
                    .initial(leads.indexOf(user.blockLead)); // initial value of the player

            for (int value : leads) {
                item.add(leads.indexOf(value),
                        config.getFromItemData(locale, "options.lead", Integer.toString(user.blockLead)),
                        (menu, event) -> user.blockLead = value);
            }

            main.item(9, item);
        }

        // opens the menu
        main
                .distributeRowEvenly(0, 1)

                .item(28, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> user.getPlayer().closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation())
                .open(player);
    }

    public static void openSingleStyle(ParkourPlayer user, StyleType styleType, ParkourOption... disabledOptions) {
        // init generic values
        Configuration config = WITP.getConfiguration();

        // init menu
        PagedMenu style = new PagedMenu(4, config.getString("items", "locale." + user.locale + ".general.menu.name"));

        List<MenuItem> items = new ArrayList<>();
        for (String name : styleType.styles.keySet()) {
            Material item = Util.getRandom(styleType.styles.get(name));

            items.add(new Item(item, "<gradient:#238681>" + name + "</gradient:#1DC9C1>")
                    .glowing(user.style.equals(name))
                    .click((menu, event) -> {
                            user.style = name;
                            user.sendTranslated("selected-style", name);
                    }));
        }

        style
                .displayRows(0, 1)
                .addToDisplay(items)

                .nextPage(27, new Item(Material.LIME_DYE, "<#0DCB07>»") // next page
                        .click((menu, event) -> style.page(1)))

                .prevPage(35, new Item(Material.RED_DYE, "<#DE1F1F>«") // previous page
                        .click((menu, event) -> style.page(-1)))

                .item(31, config.getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> openMainMenu(user, disabledOptions)))

                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new RandomAnimation());
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