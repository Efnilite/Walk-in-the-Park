package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.SessionVisibility;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.vilib.inventory.item.AutoSliderItem;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class SpectatorMenu {

    public static void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LANG.get() : user.getLocale();

        PagedMenu spectator = new PagedMenu(4, "<white>Select a player");

        List<MenuItem> display = new ArrayList<>();

        for (Session session : Session.getSessions()) {
            if (session.getVisibility() != SessionVisibility.PUBLIC) { // only showcase public
                continue;
            }

            if (user != null && session.getSessionId().equals(user.getSessionId())) { // dont let player join their own session
                continue;
            }

            AutoSliderItem slider = new AutoSliderItem(1, spectator) // todo
                    .initial(0); // slideritem

            int index = 0;
            for (ParkourPlayer pp : session.getPlayers()) {
                Item item = IP.getConfiguration().getFromItemData(locale,
                        "gamemodes.spectator-head", pp.getName());

//                List<String> lore = item.getLore(); todo add admin view
//                lore.add("");
//                lore.add("<gray><italic>You can see this because you're an admin.");
//                item.lore(lore);

                // Player head gathering
                item.material(Material.PLAYER_HEAD);

                ItemStack stack = item.build(); // Updating meta requires building
                stack.setType(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) stack.getItemMeta();
                if (meta == null) {
                    continue;
                }
                SkullSetter.setPlayerHead(pp.getPlayer(), meta);
                item.meta(meta);

                slider.add(index, item, (event) -> ParkourSpectator.spectateSession(player, session));
            }

            display.add(slider);
        }

        spectator
                .displayRows(0, 1)
                .addToDisplay(display)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> spectator.page(1)))

                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> spectator.page(-1)))

                .item(31, IP.getConfiguration().getFromItemData(locale, "general.close")
                        .click(event -> player.closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleInAnimation())
                .open(player);

    }

}
