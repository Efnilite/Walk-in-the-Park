package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.vilib.inventory.item.AutoSliderItem;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.SkullSetter;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class SpectatorMenu {

    public static void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.DEFAULT_LOCALE : user.getLocale();

        PagedMenu spectator = new PagedMenu(4, "<white>" +
                ChatColor.stripColor(IP.getConfiguration().getString("items", "locale." + locale + ".main.spectator.name")));

        List<MenuItem> display = new ArrayList<>();

        for (Session session : Session.getSessions()) {
            if (!session.isAcceptingSpectators()) { // only showcase sessions with spectators enabled
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

                // bedrock has no player skull support
                if (!Util.isBedrockPlayer(player) || player.getName().startsWith(".")) { // bedrock players' names with geyser start with a .) {
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(pp.getPlayer(), meta);
                        item.meta(meta);
                    }
                }

                slider.add(index, item, (event) -> Gamemodes.SPECTATOR.create(player, session));
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
                        .click(event -> MainMenu.INSTANCE.open(event.getPlayer())))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleInAnimation())
                .open(player);

    }

}
