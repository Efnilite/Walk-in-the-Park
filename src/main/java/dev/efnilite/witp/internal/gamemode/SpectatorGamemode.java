package dev.efnilite.witp.internal.gamemode;

import dev.efnilite.fycore.inventory.Menu;
import dev.efnilite.fycore.inventory.PagedMenu;
import dev.efnilite.fycore.inventory.animation.SplitMiddleInAnimation;
import dev.efnilite.fycore.inventory.item.Item;
import dev.efnilite.fycore.inventory.item.MenuItem;
import dev.efnilite.fycore.util.SkullSetter;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.Gamemode;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SpectatorGamemode implements Gamemode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return WITP.getConfiguration().getFromItemData(locale, "gamemodes.spectator");
    }

    @Override
    public void handleItemClick(Player player, ParkourUser user, Menu previousMenu) {
        PagedMenu spectator = new PagedMenu(3, "Select a player");
        player.closeInventory();

        List<MenuItem> display = new ArrayList<>();

        for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
            if (pp == null) {
                continue;
            }
            Player pl = pp.getPlayer();
            if (pl.getUniqueId() != player.getUniqueId() && !player.getName().equals(pl.getName())) {
                ItemStack item = WITP.getConfiguration().getFromItemData(user.locale, "gamemodes.spectator-head", pl.getName(), pl.getName()).build();
                item.setType(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta == null) {
                    continue;
                }
                SkullSetter.setPlayerHead(pl, meta);
                item.setItemMeta(meta);

                // todo add bukkit -> Item support
//                display.add(new Item(item).click((menu, event) -> {
//                    if (ParkourUser.getActivePlayers().contains(pp)) {
//                        new ParkourSpectator(user, pp, user.getPreviousData());
//                    }
//                }));

            }
        }

        spectator
                .displayRows(0, 1)

                .item(26, WITP.getConfiguration().getFromItemData(user.locale, "general.close")
                        .click((menu, event) -> {
//                            ParkourMenu.openMainMenu(user);
                        }))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SplitMiddleInAnimation())
                .open(player);
    }
}
