package dev.efnilite.ip.hook;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class FloodgateHook {

    /**
     * Whether this player is a bedrock player.
     *
     * @param player The player.
     * @return True if this player is a bedrock player, false if not.
     */
    public boolean isBedrockPlayer(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }
}