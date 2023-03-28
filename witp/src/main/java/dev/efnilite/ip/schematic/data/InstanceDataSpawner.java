package dev.efnilite.ip.schematic.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles sign data processing
 */
public class InstanceDataSpawner implements InstanceData {

    @Override
    public @Nullable String serialize(BlockData data) {
        if (!(data instanceof CreatureSpawner spawner)) {
            return null;
        }

        return IP.getGson().toJson(new SpawnDataContainer(spawner.getSpawnedType(), spawner.getDelay(),
                spawner.getMinSpawnDelay(), spawner.getMaxSpawnDelay(), spawner.getMaxNearbyEntities(),
                spawner.getRequiredPlayerRange(), spawner.getSpawnCount(), spawner.getSpawnRange()));
    }

    @Override
    public @NotNull BlockData deserialize(BlockData data, String extra) {
        CreatureSpawner spawner = (CreatureSpawner) data;

        SpawnDataContainer container = IP.getGson().fromJson(extra, SpawnDataContainer.class);
        spawner.setSpawnedType(container.spawnedType);
        spawner.setDelay(container.delay);
        spawner.setMinSpawnDelay(container.minDelay);
        spawner.setMaxSpawnDelay(container.maxDelay);
        spawner.setMaxNearbyEntities(container.maxNearby);
        spawner.setRequiredPlayerRange(container.playerRange);
        spawner.setSpawnCount(container.spawnCount);
        spawner.setSpawnRange(container.spawnRange);

        return (BlockData) spawner;
    }

    private record SpawnDataContainer(@Expose EntityType spawnedType, @Expose int delay, @Expose int minDelay, @Expose int maxDelay,
                                      @Expose int maxNearby, @Expose int playerRange, @Expose int spawnCount, @Expose int spawnRange) {}
}