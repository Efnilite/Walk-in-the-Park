package dev.efnilite.ip.menu.old;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class MainMenu extends DynamicMenu {

    public static final MainMenu INSTANCE = new MainMenu();

    public MainMenu() {
        // Singleplayer if player is not found
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.singleplayer").click(
                event -> Menus.SINGLE.open(event.getPlayer())),
                ParkourOption.JOIN::check);

        registerMainItem(1, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.spectator").click(
                event -> Menus.SPECTATOR.open(event.getPlayer())),
                // display spectator if the player isn't already one
                ParkourOption.JOIN::check);

        // Settings if player is active
        registerMainItem(1, 9,
                user -> IP.getConfiguration().getFromItemData(user, "main.settings").click(event -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                if (pp != null) {
                    pp.getGenerator().menu();
                }
        }), player -> ParkourPlayer.isPlayer(player) && ParkourOption.SETTINGS.check(player));

        // Quit button if player is active
        registerMainItem(1, 10,
                user -> IP.getConfiguration().getFromItemData(user, "main.quit").click(event ->
                ParkourUser.leave(event.getPlayer())),
                ParkourPlayer::isPlayer);

        // Leaderboard only if player has perms
        registerMainItem(3, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.leaderboard").click(
                event -> Menus.LEADERBOARDS.open(event.getPlayer())),
                ParkourOption.LEADERBOARD::check);

        // Language only if player has perms
        registerMainItem(3, 1,
                user -> IP.getConfiguration().getFromItemData(user, "main.language").click(
                event -> Menus.LANG.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                player -> ParkourPlayer.isPlayer(player) && ParkourOption.LANGUAGE.check(player));

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
        Menu menu = new Menu(4, "<white>Parkour")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .distributeRowEvenly(0, 1, 2, 3);

        display(player, menu);
    }
}