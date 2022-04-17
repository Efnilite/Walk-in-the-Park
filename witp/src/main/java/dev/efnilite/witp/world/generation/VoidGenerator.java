package dev.efnilite.witp.world.generation;

import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper class of the specific version void generators
 *
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
        if (Version.isHigherOrEqual(Version.V1_17)) {
            return new VoidGenerator_v1_17();
        } else {
            return new VoidGenerator_v1_16();
        }
    }

    /**
     * Returns the active void generator plugin
     *
     * @return the void generator currently in use
     */
    public static @Nullable String getMultiverseGenerator() {
        if (Bukkit.getPluginManager().getPlugin("WVoidGen") != null) {
            Logging.error("WVoidGen is outdated since v3.0.4.");
            Logging.error("Please delete the jar file and visit the wiki for more info.");
            return null;
        }

        if (Bukkit.getPluginManager().getPlugin("VoidGen") != null) {
            return "VoidGen";
        } else {
            return null;
        }
    }
}