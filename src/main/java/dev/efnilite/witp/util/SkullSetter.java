package dev.efnilite.witp.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

public class SkullSetter {
    private static boolean isPaper;

    static {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            isPaper = false;
        }
    }

    @SuppressWarnings("deprecation")
    public static void setPlayerHead(Player player, SkullMeta meta) {
        if (!Version.isHigherOrEqual(Version.V1_12)) {
            meta.setOwner(player.getName());
        } else if (isPaper) {
            if (player.getPlayerProfile().hasTextures()) {
                meta.setPlayerProfile(player.getPlayerProfile());
            }
        } else {
            meta.setOwningPlayer(player);
        }
    }
}
