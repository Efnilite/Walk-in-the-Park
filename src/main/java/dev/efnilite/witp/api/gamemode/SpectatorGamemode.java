package dev.efnilite.witp.api.gamemode;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourSpectator;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class SpectatorGamemode implements Gamemode {

    @Override
    public String getName() {
        return "spectator";
    }

    @Override
    public ItemStack getItem() {
        return WITP.getConfiguration().getFromItemData("gamemodes.spectator");
    }

    @Override
    public void handleItemClick(Player player, ParkourUser user, InventoryBuilder previousInventory) {
        InventoryBuilder spectatable = new InventoryBuilder(user, 3, "Select a player").open();
        int index = 0;
        player.closeInventory();
        for (ParkourPlayer pp : ParkourUser.getActivePlayers()) {
            if (pp == null || pp.getGenerator() == null) {
                continue;
            }
            Player pl = pp.getPlayer();
            if (pl.getUniqueId() != player.getUniqueId() && !player.getName().equals(pl.getName())) {
                ItemStack item = WITP.getConfiguration().getFromItemData("gamemodes.spectator-head", pl.getName(), pl.getName());
                item.setType(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta == null) {
                    continue;
                }
                meta.setOwningPlayer(pl);
                item.setItemMeta(meta);
                spectatable.setItem(index, item, (t2, e2) -> {
                    if (ParkourUser.getActivePlayers().contains(pp) && pp.getGenerator() != null) {
                        new ParkourSpectator(user, pp);
                    }
                });
                index++;
                if (index == 25) {
                    break;
                }
            }
        }
        spectatable.setItem(25, WITP.getConfiguration().getFromItemData("gamemodes.search"),
                (t2, e2) -> {
                    player.closeInventory();
                    BaseComponent[] send = new ComponentBuilder().append(user.getTranslated("click-search"))
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/witp search ")).create();
                    player.spigot().sendMessage(send);
                });
        spectatable.setItem(26, WITP.getConfiguration().getFromItemData("general.close"), (t2, e2) -> previousInventory.build());
        spectatable.build();
    }
}
