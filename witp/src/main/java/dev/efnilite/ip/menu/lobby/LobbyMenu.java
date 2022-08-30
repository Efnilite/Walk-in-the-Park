package dev.efnilite.ip.menu.lobby;

import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.SessionVisibility;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.vilib.inventory.item.SliderItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyMenu extends DynamicMenu {

    public LobbyMenu() {
        registerMainItem(1, 0,
            (player, user) -> Locales.getItem(player, "lobby.player_management.item")
                    .click(event -> Menus.PLAYER_MANAGEMENT.open(player)),
            player -> {
                ParkourUser user = ParkourUser.getUser(player);

                return ParkourOption.PLAYER_MANAGEMENT.checkPermission(player) &&
                        user instanceof ParkourPlayer &&
                        user.getSession().getPlayers().get(0) == user;
            }
        );

        registerMainItem(1, 1,
            (player, user) -> {
                assert user != null;

                List<String> values = Locales.getStringList(user.getLocale(), "lobby.visibility.values", false);

                return new SliderItem()
                    .initial(switch (user.getSession().getVisibility()) {
                        case PUBLIC -> 0;
                        case ID_ONLY -> 1;
                        case PRIVATE -> 2;
                    })
                    .add(0, Locales.getItem(player, "lobby.visibility")
                            .modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                        ParkourUser u = ParkourUser.getUser(event.getPlayer());

                        if (u != null) {
                            u.getSession().setVisibility(SessionVisibility.PUBLIC);
                        }

                        return true;
                    }).add(1, Locales.getItem(player, "lobby.visibility")
                            .modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // id only
                        ParkourUser u = ParkourUser.getUser(event.getPlayer());

                        if (u != null) {
                            u.getSession().setVisibility(SessionVisibility.ID_ONLY);
                        }

                        return true;
                    }).add(2, Locales.getItem(player, "lobby.visibility")
                            .modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // private
                        ParkourUser u = ParkourUser.getUser(event.getPlayer());

                        if (u != null) {
                            u.getSession().setVisibility(SessionVisibility.PRIVATE);
                        }

                        return true;
                    });
            },
            player -> {
                ParkourUser user = ParkourUser.getUser(player);

                return ParkourOption.VISIBILITY.checkPermission(player) &&
                        user instanceof ParkourPlayer &&
                        user.getSession().getPlayers().get(0) == user;
            });

        // Always allow closing of the menu
        registerMainItem(2, 10,
                (player, user) -> Locales.getItem(player, "other.close")
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
        Menu menu = new Menu(3, Locales.getString(player, "lobby.name", false))
            .fillBackground(Material.WHITE_STAINED_GLASS_PANE)
            .animation(new SplitMiddleInAnimation())
            .distributeRowsEvenly();

        display(player, menu);
    }
}