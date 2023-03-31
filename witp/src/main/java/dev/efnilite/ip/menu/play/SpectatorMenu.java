package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.api.Modes;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.vilib.inventory.PagedMenu;
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

/**
 * The menu to select other players to spectate
 */
public class SpectatorMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.getLocale();

        PagedMenu spectator = new PagedMenu(3, Locales.getString(player, "play.spectator.name"));

        List<MenuItem> display = new ArrayList<>();

        for (Session session : WorldDivider.SESSIONS.values()) {
            if (!session.isAcceptingSpectators.apply(session)) { // only showcase sessions with spectators enabled
                continue;
            }

            if (user != null && session == user.session) { // don't let player join their own session
                continue;
            }

            AutoSliderItem slider = new AutoSliderItem(1, spectator).initial(0); // slideritem

            int index = 0;
            for (ParkourPlayer pp : session.getPlayers()) {
                Item item = Locales.getItem(locale, "play.spectator.head", pp.getName());
                // Player head gathering
                item.material(Material.PLAYER_HEAD);

                ItemStack stack = item.build(); // Updating meta requires building
                stack.setType(Material.PLAYER_HEAD);

                // bedrock has no player skull support
                if (!Util.isBedrockPlayer(player)) {
                    if (pp.getName() != null && !pp.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                        SkullMeta meta = (SkullMeta) stack.getItemMeta();

                        if (meta != null) {
                            SkullSetter.setPlayerHead(pp.player, meta);
                            item.meta(meta);
                        }
                    }
                }

                slider.add(index, item, (event) -> Modes.SPECTATOR.create(player, session));
            }

            display.add(slider);
        }

        spectator.displayRows(0, 1)
                .addToDisplay(display)
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT) // next page
                        .click(event -> spectator.page(1)))
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT) // previous page
                        .click(event -> spectator.page(-1)))
                .item(22, Locales.getItem(player, "other.close").click(event -> Menus.PLAY.open(event.getPlayer())))
                .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE)
                .open(player);

    }

}
