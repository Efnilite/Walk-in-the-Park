package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class PlayMenu extends DynamicMenu {

    public static final PlayMenu INSTANCE = new PlayMenu();

    public PlayMenu() {
        // Singleplayer if player is not found
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.singleplayer").click(
                event -> SingleplayerMenu.open(event.getPlayer())),
                ParkourOption.JOIN::check);

        registerMainItem(1, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.spectator").click(
                event -> SpectatorMenu.open(event.getPlayer())),
                // display spectator if the player isn't already one
                ParkourOption.JOIN::check);

        // Always allow closing of the menu
        registerMainItem(3, 10,
                user -> IP.getConfiguration().getFromItemData(user, "general.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    /**
     * Opens the main menu.
     *
     * @param   player
     *          The player to open the menu to
     */
    public void open(Player player) {
        Menu menu = new Menu(4, "<white>Play")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .distributeRowEvenly(0, 1, 2, 3);

        display(player, menu);
    }
}