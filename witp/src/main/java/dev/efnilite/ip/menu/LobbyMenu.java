package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.SessionVisibility;
import dev.efnilite.ip.session.chat.ChatType;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
import dev.efnilite.vilib.inventory.item.SliderItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyMenu extends DynamicMenu {

    public static final LobbyMenu INSTANCE = new LobbyMenu();

    public LobbyMenu() {
        registerMainItem(1, 9,
                user -> {
                    List<String> values = IP.getConfiguration().getStringList("items", "locale." + user.getLocale() + ".lobby.visibility.values");
                    return new SliderItem()
                            .initial(switch (user.getSession().getVisibility()) {
                                case PUBLIC -> 0;
                                case ID_ONLY -> 1;
                                case PRIVATE -> 2;
                            })
                            .add(0, IP.getConfiguration().getFromItemData(user, "lobby.visibility")
                                    .modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.getSession().setVisibility(SessionVisibility.PUBLIC);
                                }

                                return true;
                            }).add(1, IP.getConfiguration().getFromItemData(user, "lobby.visibility")
                                    .modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // id only
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.getSession().setVisibility(SessionVisibility.ID_ONLY);
                                }

                                return true;
                            }).add(2, IP.getConfiguration().getFromItemData(user, "lobby.visibility")
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

                    return ParkourOption.VISIBILITY.check(player) &&
                            user != null &&
                            user.getSession().getPlayers().get(0).getPlayer() == player; // only if player is the owner
                });

        registerMainItem(1, 10,
                user -> {
                    List<String> values = IP.getConfiguration().getStringList("items", "locale." + user.getLocale() + ".lobby.chat.values");
                    return new SliderItem()
                            .initial(
                                switch (user.getChatType()) {
                                    case LOBBY_ONLY -> 0;
                                    case PLAYERS_ONLY -> 1;
                                    case PUBLIC -> 2;
                                }) // user has to be not-null to see this item
                            .add(0, IP.getConfiguration().getFromItemData(user, "lobby.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // lobby only
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.setChatType(ChatType.LOBBY_ONLY);
                                }

                                return true;
                            }).add(1, IP.getConfiguration().getFromItemData(user, "lobby.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // players only
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.setChatType(ChatType.PLAYERS_ONLY);
                                }

                                return true;
                            }).add(2, IP.getConfiguration().getFromItemData(user, "lobby.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.setChatType(ChatType.PUBLIC);
                                }

                                return true;
                            });
                },
                player -> ParkourOption.CHAT.check(player) && ParkourUser.getUser(player) != null);

        // Always allow closing of the menu
        registerMainItem(2, 10,
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
        Menu menu = new Menu(3, "<white>Lobby")
                .fillBackground(Material.WHITE_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .distributeRowEvenly(0, 1, 2);

        display(player, menu);
    }
}