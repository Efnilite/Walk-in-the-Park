package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * The menu where players can join modes
 */
public class PlayMenu extends DynamicMenu {

    public PlayMenu() {
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.singleplayer")
                        .click(event -> Menus.SINGLE.open(event.getPlayer())),
                player -> true);

        registerMainItem(1, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.spectator")
                        .click(event -> Menus.SPECTATOR.open(event.getPlayer())),
                player -> true);

        registerMainItem(2, 0,
                user -> IP.getConfiguration().getFromItemData(user, "general.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    public void open(Player player) {
        Menu menu = new Menu(3, "<white>Play")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .distributeRowsEvenly();

        display(player, menu);
    }
}