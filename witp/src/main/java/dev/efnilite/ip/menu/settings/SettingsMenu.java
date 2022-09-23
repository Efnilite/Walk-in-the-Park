package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.chat.ChatType;
import dev.efnilite.ip.util.Util;
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
                (player, user) -> Locales.getItem(player, "settings.parkour_settings.item")
                        .click(event -> {
                                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                                if (pp != null) {
                                    pp.getGenerator().menu();
                                }
                        }),
                player -> ParkourOption.PARKOUR_SETTINGS.check(player) && ParkourUser.isPlayer(player));

        registerMainItem(1, 1,
                (player, user) -> Locales.getItem(player, "settings.lang.item", user != null ? user.getLocale() : "?")
                        .click(event -> Menus.LANG.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                player -> ParkourOption.LANG.check(player) && ParkourUser.isUser(player));

        registerMainItem(1, 2,
                (player, user) -> {
                    // user has to be not-null to see this item
                    assert user != null;

                    List<String> values = Locales.getStringList(user.getLocale(), "settings.chat.values", false);

                    return new SliderItem()
                            .initial(switch (user.chatType) {
                                        case LOBBY_ONLY -> 0;
                                        case PLAYERS_ONLY -> 1;
                                        case PUBLIC -> 2;
                                    })
                            .add(0, Locales.getItem(player, "settings.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // lobby only
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.chatType = ChatType.LOBBY_ONLY;
                                }

                                return true;
                            }).add(1, Locales.getItem(player, "settings.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // players only
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.chatType = ChatType.PLAYERS_ONLY;
                                }

                                return true;
                            }).add(2, Locales.getItem(player, "settings.chat")
                                    .modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                                if (u != null) {
                                    u.chatType = ChatType.PUBLIC;
                                }

                                return true;
                            });
                },
                player -> ParkourOption.CHAT.check(player) && ParkourUser.isUser(player));

        registerMainItem(2, 0,
                (player, user) -> Locales.getItem(player, "other.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    public void open(Player player) {
        Menu menu = new Menu(3, Locales.getString(player, "settings.name", false))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.GRAY_STAINED_GLASS_PANE : Material.AIR)
                .animation(new SplitMiddleOutAnimation())
                .distributeRowsEvenly();

        display(player, menu);
    }
}