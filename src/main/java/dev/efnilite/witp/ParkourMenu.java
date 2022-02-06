package dev.efnilite.witp;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.animation.SnakeSingleAnimation;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.player.ParkourUser;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class ParkourMenu {

    public static void openMainMenu(ParkourUser user, String... optDisabled)  {
        List<String> disabled = Arrays.asList(optDisabled);

        Menu mainMenu = new Menu(3, getInventoryName(user, "general.menu"))
                .distributeRowEvenly(2)
                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation());

        if (checkOptions("lead", "witp.lead", disabled)) {

        }

        mainMenu.open(user.getPlayer());
    }

    private static String getInventoryName(ParkourUser user, String type) {
        String name = WITP.getConfiguration().getString("items", "locale." + user.locale + "." + type.toLowerCase() + ".name");
        if (name == null) {
            Logging.warn("Didn't find a value for option '" + "locale." + user.locale + "." + type.toLowerCase() + ".name'");
            return "";
        }
        return ChatColor.stripColor(name);
    }


}
