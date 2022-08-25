package dev.efnilite.ip.menu.lobby;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.session.Session;
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

        ParkourPlayer player = ParkourPlayer.getPlayer(p);

        if (player == null) {
            return;
        }

        Session session = player.getSession();

        PagedMenu menu = new PagedMenu(3, "<white>Manage Players");

        for (ParkourPlayer sessionPlayer : session.getPlayers()) {
            if (sessionPlayer == player) {
                continue;
            }

            Player sessionBukkitPlayer = sessionPlayer.player;
            Item item = new Item(Material.PLAYER_HEAD, "#4BD16D<bold>" + player.getName());

            boolean muted = session.isMuted(sessionPlayer);

            List<String> lore = new ArrayList<>();
            if (muted) {
                lore.add("<dark_gray>Muted in lobby");
                lore.add("");
                lore.add("<#8DE5A3>Left click<dark_gray> to kick from lobby");
                lore.add("<#8DE5A3>Right click<dark_gray> to unmute in lobby");
            } else {
                lore.add("<#8DE5A3>Left click<dark_gray> to kick from lobby");
                lore.add("<#8DE5A3>Right click<dark_gray> to mute in lobby");
            }

            // Player head gathering
            item
                    .material(Material.PLAYER_HEAD)
                    .lore(lore)
                    .click(event -> {
                        ClickType click = event.getEvent().getClick();

                        switch (click) {
                            case LEFT -> {
                                IP.getDivider().generate(ParkourPlayer.register(sessionBukkitPlayer));
                                sessionPlayer.send(IP.PREFIX + "You've been kicked from your previous lobby by the lobby owner.");

                                player.send(IP.PREFIX + "Set your lobby visibility to invite-only to avoid people randomly joining.");
                                open(p);
                            }
                            case RIGHT -> {
                                session.setMuted(sessionPlayer, !muted);

                                if (!muted) {
                                    sessionPlayer.send(IP.PREFIX + "You've been muted by the lobby owner.");
                                } else {
                                    sessionPlayer.send(IP.PREFIX + "You've been unmuted by the lobby owner.");
                                }

                                open(p);
                            }
                        }
                    });

            ItemStack stack = item.build(); // Updating meta requires building
            stack.setType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (!Util.isBedrockPlayer(sessionBukkitPlayer)) {
                if (sessionPlayer.getName() != null && !sessionPlayer.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(sessionBukkitPlayer, meta);
                        item.meta(meta);
                    }
                }
            }

            menu.addToDisplay(List.of(item));
        }

        menu
                .displayRows(0, 1)
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT)
                        .click(event -> menu.page(-1)))
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT)
                        .click(event -> menu.page(1)))
                .item(22, IP.getConfiguration().getFromItemData(player, "general.close")
                        .click(event -> event.getPlayer().closeInventory()))
                .open(p);
    }
}