package dev.efnilite.ip.schematic.state;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles data that is not included in {@link BlockData} but is useful in schematics.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public interface State {

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

    /**
     * @param data The data.
     * @return The {@link State} instance which saves this data. Returns null if no implementation is found.
     */
    @Nullable
    static State getState(BlockData data) {
        // check special states
        if (data instanceof Sign) {
            return new StateSign();
        } else if (data instanceof Skull) {
            return new StateSkull();
        } else if (data instanceof CreatureSpawner) {
            return new StateCreatureSpawner();
        } else {
            return null;
        }
    }
}