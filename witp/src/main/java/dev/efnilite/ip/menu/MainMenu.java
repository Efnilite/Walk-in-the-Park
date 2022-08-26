package dev.efnilite.ip.menu;

import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MainMenu extends DynamicMenu {

    public MainMenu() {
        registerMainItem(1, 0,
                (player, user) -> Locales.getItem(player, "play.item")
                        .click(event -> Menus.PLAY.open(event.getPlayer())),
                ParkourOption.PLAY::check);

        registerMainItem(1, 1,
                (player, user) -> Locales.getItem(player, "community.item")
                        .click(event -> Menus.COMMUNITY.open(event.getPlayer())),
                ParkourOption.COMMUNITY::check);

        registerMainItem(1, 2,
                (player, user) -> Locales.getItem(player, "settings.item")
                        .click(event -> Menus.SETTINGS.open(event.getPlayer())),
                ParkourOption.SETTINGS::check);

        registerMainItem(1, 3,
                (player, user) -> Locales.getItem(player, "lobby.item")
                        .click(event -> Menus.LOBBY.open(event.getPlayer())),
                ParkourOption.LOBBY::check);

        registerMainItem(1, 4,
                (player, user) -> Locales.getItem(player, "other.quit")
                        .click(event -> ParkourPlayer.leave(player)),
                player -> true);
    }

    public void open(Player player) {
        Menu menu = new Menu(3, Locales.getString(player, "main.name"))
                .distributeRowsEvenly()
                .fillBackground(Material.WHITE_STAINED_GLASS_PANE)
                .animation(new RandomAnimation());

        display(player, menu);
    }

}
