package dev.efnilite.ip.world;

import dev.efnilite.vilib.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Wrapper class of the specific version void generators
 * Inspired by the VoidGen plugin.
 */
public class VoidGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    public static VoidGenerator getGenerator() {
        return Version.isHigherOrEqual(Version.V1_17) ? new VoidGenerator_v1_17() : new VoidGenerator_v1_16();
    }

    /**
     * Returns the active void generator plugin
     *
     * @return the void generator currently in use
     */
    public static @Nullable String getMultiverseGenerator() {
        return Bukkit.getPluginManager().getPlugin("VoidGen") != null ? "VoidGen" : null;
    }

    @SuppressWarnings("deprecation")
    protected static class VoidGenerator_v1_16 extends VoidGenerator {

        @NotNull
        @Override
        public ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.BiomeGrid biome) {
            return createChunkData(world);
        }
    }

    protected static class VoidGenerator_v1_17 extends VoidGenerator {

        @Override
        public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
            return new VoidGenerator_v1_17.VoidBiomeProvider();
        }

        private static class VoidBiomeProvider extends BiomeProvider {

            @Override
            @NotNull
            public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
                return Biome.PLAINS;
            }

            @Override
            @NotNull
            public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
                return List.of(Biome.PLAINS);
            }
        }

    }
}