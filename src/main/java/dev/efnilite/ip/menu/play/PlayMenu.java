package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * The menu where players can join modes
 */
public class PlayMenu extends DynamicMenu {

    public PlayMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.single.item").click(event -> Menus.SINGLE.open(event.getPlayer())), ParkourOption.SINGLE::mayPerform);
        registerMainItem(1, 2, (player, user) -> Locales.getItem(player, "play.spectator.item").click(event -> Menus.SPECTATOR.open(event.getPlayer())), ParkourOption.SPECTATOR::mayPerform);
        registerMainItem(2, 0, (player, user) -> Locales.getItem(player, "other.close").click(event -> event.getPlayer().closeInventory()), player -> true);
    }

    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "play.name"))
                .fillBackground(ParkourUser.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .distributeRowsEvenly());
    }
}