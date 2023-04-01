package dev.efnilite.ip.menu.lobby;

import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.vilib.inventory.item.SliderItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyMenu extends DynamicMenu {

    public LobbyMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "lobby.player_management.item").click(event -> Menus.PLAYER_MANAGEMENT.open(player)), player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.PLAYER_MANAGEMENT.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(1, 1, (player, user) -> {
            if (user == null) {
                return null;
            }

            List<String> values = Locales.getStringList(user.locale, "lobby.visibility.values");

            return new SliderItem().initial(switch (user.session.visibility) {
                case PUBLIC -> 0;
                case ID_ONLY -> 1;
                case PRIVATE -> 2;
            }).add(0, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.visibility = Session.Visibility.PUBLIC;
                }

                return true;
            }).add(1, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // id only
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.visibility = Session.Visibility.ID_ONLY;
                }

                return true;
            }).add(2, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // private
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.visibility = Session.Visibility.PRIVATE;
                }

                return true;
            });
        }, player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.VISIBILITY.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(2, 10, (player, user) -> Locales.getItem(player, "other.close").click(event -> event.getPlayer().closeInventory()), player -> true);
    }

    /**
     * Opens the main menu.
     *
     * @param player The player to open the menu to
     */
    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "lobby.name"))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.WHITE_STAINED_GLASS_PANE)
                .animation(new SplitMiddleInAnimation())
                .distributeRowsEvenly());
    }
}