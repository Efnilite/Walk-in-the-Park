package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.nio.file.Files;

/**
 * Minecraft world manager.
 */
public class WorldManagerMC implements WorldManager {

    @Override
    public World createWorld() {
        World world = null;

        try {
            WorldCreator creator = new WorldCreator(Option.WORLD_NAME)
                    .generateStructures(false)
                    .type(WorldType.NORMAL)
                    .generator(Version.isHigherOrEqual(Version.V1_17) ? new VoidGenerator.VoidGenerator_v1_17() : new VoidGenerator.VoidGenerator_v1_16()) // to fix No keys in MapLayer etc.
                    .environment(World.Environment.NORMAL);

            world = Bukkit.createWorld(creator);
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to create the parkour world", "delete the parkour world folder and restart the server", ex);
        }
        return world;
    }

    @Override
    public void deleteWorld() {
        Bukkit.unloadWorld(Option.WORLD_NAME, false);

        try {
            Files.delete(new File(Option.WORLD_NAME).toPath());
        } catch (Exception ex) {
            IP.logging().warn("Error while trying to delete parkour world: %s".formatted(ex.getMessage()));
        }
    }
}