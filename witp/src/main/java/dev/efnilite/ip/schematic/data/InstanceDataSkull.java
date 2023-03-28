package dev.efnilite.ip.schematic.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Handles skull data processing
 */
public class InstanceDataSkull implements InstanceData {

    @Override
    public @Nullable String serialize(BlockData data) {
        if (!(data instanceof Skull skull)) {
            return null;
        }

        PlayerProfile profile = skull.getOwnerProfile();

        return profile != null ? IP.getGson().toJson(new SkullDataContainer(profile.serialize())) : null;
    }

    @Override
    public @NotNull BlockData deserialize(BlockData data, String extra) {
        Skull skull = (Skull) data;

        SkullDataContainer container = IP.getGson().fromJson(extra, SkullDataContainer.class);
        System.out.println(container.profile.keySet() + " || " + container.profile.values()); // todo what does this return and how tf do i deserialize it
//        skull.setOwnerProfile(container.profile);

        return (BlockData) skull;
    }

    private record SkullDataContainer(@Expose Map<String, Object> profile) {}
}