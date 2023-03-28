package dev.efnilite.ip.schematic.data;

import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles data that is not included in {@link BlockData} but is useful in schematics.
 */
public interface InstanceData {

    /**
     * @param data The {@link BlockData} to serialize.
     * @return The serialized data. May be null.
     */
    @Nullable
    String serialize(BlockData data);

    /**
     * @param data The existing {@link BlockData}, excluding instance data.
     * @param extra The serialized data string.
     * @return The updated {@link BlockData} instance.
     */
    @NotNull
    BlockData deserialize(BlockData data, String extra);

}