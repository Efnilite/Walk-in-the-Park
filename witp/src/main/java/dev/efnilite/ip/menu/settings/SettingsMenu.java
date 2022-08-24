package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.chat.ChatType;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleOutAnimation;
import dev.efnilite.vilib.inventory.item.SliderItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The menu for settings
 */
public class SettingsMenu extends DynamicMenu {

    public SettingsMenu() {
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.settings")
                        .click(event -> {
                                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                                if (pp != null) {
                                    pp.getGenerator().menu();
                                }
                        }),
                ParkourPlayer::isPlayer);

        registerMainItem(1, 1,
                user -> IP.getConfiguration().getFromItemData(user, "main.language")
                        .click(event -> Menus.LANG.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                ParkourPlayer::isPlayer);

        registerMainItem(1, 2,
                user -> {
                    // user has to be not-null to see this item
                    List<String> values = IP.getConfiguration().getStringList("items", "locale." + user.getLocale() + ".lobby.chat.values");

                    return new SliderItem()
                            .initial(switch (user.getChatType()) {
                                        case LOBBY_ONLY -> 0;
                                        case PLAYERS_ONLY -> 1;
                                        case PUBLIC -> 2;
                                    })
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
                ParkourUser::isUser);
    }

    public void open(Player player) {
        Menu menu = new Menu(4, "<white>Settings")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .distributeRowEvenly(0, 1, 2, 3);

        display(player, menu);
    }
}