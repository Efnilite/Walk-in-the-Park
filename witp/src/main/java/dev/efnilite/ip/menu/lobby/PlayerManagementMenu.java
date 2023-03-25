package dev.efnilite.ip.menu.lobby;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session2;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for managing players
 */
public class PlayerManagementMenu {

    public void open(Player p) {
        if (p == null) {
            return;
        }

        ParkourPlayer viewer = ParkourPlayer.getPlayer(p);

        if (viewer == null) {
            return;
        }

        Session2 session = viewer.session;

        PagedMenu menu = new PagedMenu(3, Locales.getString(viewer.getLocale(), "lobby.player_management.name"));
        add(menu, viewer, Colls.map(player -> player, session.getPlayers()));
        add(menu, viewer, Colls.map(player -> player, session.getSpectators()));

        menu.displayRows(0, 1)
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT)
                        .click(event -> menu.page(-1)))
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT)
                        .click(event -> menu.page(1)))
                .item(22, Locales.getItem(viewer.getLocale(), "other.close")
                        .click(event -> Menus.LOBBY.open(event.getPlayer())))
                .fillBackground(Util.isBedrockPlayer(p) ? Material.AIR : Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .open(p);
    }

    private void add(PagedMenu menu, ParkourUser viewer, List<ParkourUser> users) {
        Session2 session = viewer.session;

        for (ParkourUser other : users) {
            if (other == viewer) {
                continue;
            }

            Player sessionBukkitPlayer = other.player;
            Item item = Locales.getItem(viewer.getLocale(), "lobby.player_management.head", other.getName());
            item.material(Material.PLAYER_HEAD);

            boolean muted = session.muted.contains(other);

            List<String> lore = new ArrayList<>();
            if (muted) {
                String[] top = Locales.getString(viewer.getLocale(), "lobby.player_management.head.top").split("\\|\\|");

                lore.addAll(List.of(top));
            }
            String[] bottom = Locales.getString(viewer.getLocale(), "lobby.player_management.head.bottom").split("\\|\\|");

            lore.addAll(List.of(bottom));

            // Player head gathering
            item.material(Material.PLAYER_HEAD).lore(lore).click(event -> {
                ClickType click = event.getEvent().getClick();

                switch (click) {
                    case LEFT -> {
                        Session2.Builder.create()
                                .addPlayers(ParkourUser.register(sessionBukkitPlayer))
                                .complete();

                        other.send(IP.PREFIX + Locales.getString(other.getLocale(), "lobby.player_management.kicked"));

                        viewer.send(IP.PREFIX + Locales.getString(viewer.getLocale(), "lobby.player_management.advice"));
                        open(viewer.player);
                    }
                    case RIGHT -> {
                        session.mute(other);

                        if (!muted) {
                            other.send(IP.PREFIX + Locales.getString(other.getLocale(), "lobby.player_management.muted"));
                        } else {
                            other.send(IP.PREFIX + Locales.getString(other.getLocale(), "lobby.player_management.unmuted"));
                        }

                        open(viewer.player);
                    }
                }
            });

            ItemStack stack = item.build(); // Updating meta requires building
            stack.setType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (menu.getTotalToDisplay().size() <= 36 && !Util.isBedrockPlayer(sessionBukkitPlayer)) {
                if (other.getName() != null && !other.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(sessionBukkitPlayer, meta);
                        item.meta(meta);
                    }
                }
            }

            menu.addToDisplay(List.of(item));
        }
    }
}