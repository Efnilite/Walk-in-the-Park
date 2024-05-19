package dev.efnilite.ip.menu;

import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import org.bukkit.entity.Player;

public class MainMenu extends DynamicMenu {

    public MainMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.item").click(event -> Menus.PLAY.open(event.getPlayer())), player -> ParkourOption.PLAY.mayPerform(player) && Config.CONFIG.getBoolean("joining"));
        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "community.item").click(event -> Menus.COMMUNITY.open(event.getPlayer())), ParkourOption.COMMUNITY::mayPerform);
        registerMainItem(1, 2, (player, user) -> Locales.getItem(player, "settings.item").click(event -> Menus.SETTINGS.open(event.getPlayer())), player -> ParkourOption.SETTINGS.mayPerform(player) && ParkourUser.isUser(player));
        registerMainItem(1, 3, (player, user) -> Locales.getItem(player, "lobby.item").click(event -> Menus.LOBBY.open(event.getPlayer())), player -> ParkourOption.LOBBY.mayPerform(player) && ParkourUser.isUser(player));
        registerMainItem(1, 4, (player, user) -> Locales.getItem(player, "other.quit").click(event -> ParkourPlayer2.as(player).leave(false, false), player -> ParkourOption.QUIT.mayPerform(player) && ParkourUser.isUser(player));
    }

    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "main.name"))
                .distributeRowsEvenly());
    }
}