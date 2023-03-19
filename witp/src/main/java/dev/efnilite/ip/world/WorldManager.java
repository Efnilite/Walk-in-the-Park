package dev.efnilite.ip.world;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.util.Version;
import org.bukkit.*;

import java.io.File;
import java.nio.file.Files;

public abstract class WorldManager {

    /**
     * Implementation for creating a world.
     *
     * @return The created world.
     */
    public abstract World createWorld();

    /**
     * Implementation for deleting a world.
     */
    public abstract void deleteWorld();

    /**
     * Creates a new world and sets all according settings in it.
     */
    public static void create() {
        World world = getWorld();

        if (!Option.JOINING || (!Option.DELETE_ON_RELOAD && world != null)) {
            return;
        }

        if (world != null) {
            IP.logging().warn("## ");
            IP.logging().warn("## Crash detected! Please note that the parkour world loading twice is not usual behaviour.");
            IP.logging().warn("## This only happens after a server crash.");
            IP.logging().warn("## ");
        }

        WorldManager manager = getInstance();

        manager.deleteWorld();
        world = manager.createWorld();

        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setAutoSave(false);
    }

    /**
     * Deletes the world.
     */
    public static void delete() {
        if (!Option.DELETE_ON_RELOAD || !Option.JOINING) {
            return;
        }

        getInstance().deleteWorld();
    }

    /**
     * Gets the parkour world.
     *
     * @return the Bukkit world wherein IP is currently active.
     */
    public static World getWorld() {
        return Bukkit.getWorld(Option.WORLD_NAME);
    }

    /**
     * Returns the appropriate instance.
     * @return The appropriate instance.
     */
    public static WorldManager getInstance() {
        if (WorldManagerMV.MANAGER != null) {
            return new WorldManagerMV();
        } else {
            return new WorldManagerMC();
        }
    }

    public static class WorldManagerMV extends WorldManager {

        public static MVWorldManager MANAGER;

        static {
            MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

            if (core != null) {
                MANAGER = core.getMVWorldManager();
            }
        }

        @Override
        public World createWorld() {
            if (MANAGER == null) {
                return null;
            }

            MANAGER.addWorld(Option.WORLD_NAME, World.Environment.NORMAL, null, WorldType.FLAT, false, VoidGenerator.getMultiverseGenerator());
            MultiverseWorld world = MANAGER.getMVWorld(Option.WORLD_NAME);

            // optimizations to reduce memory usage
            world.setAllowAnimalSpawn(false);
            world.setAllowMonsterSpawn(false);
            world.setKeepSpawnInMemory(false);

            return world.getCBWorld();
        }

        @Override
        public void deleteWorld() {
            if (MANAGER == null) {
                return;
            }

            MANAGER.deleteWorld(Option.WORLD_NAME, false); // deleteFromConfig
        }
    }

    public static class WorldManagerMC extends WorldManager {

        @Override
        public World createWorld() {
            World world = null;

            try {
                WorldCreator creator = new WorldCreator(Option.WORLD_NAME)
                        .generateStructures(false)
                        .type(WorldType.NORMAL)
                        .generator(Version.isHigherOrEqual(Version.V1_17)
                                ? new VoidGenerator.VoidGenerator_v1_17()
                                : new VoidGenerator.VoidGenerator_v1_16()) // to fix No keys in MapLayer etc.
                        .environment(World.Environment.NORMAL);

                world = Bukkit.createWorld(creator);
            } catch (Exception ex) {
                IP.logging().stack("Error while trying to create the parkour world",
                        "delete the parkour world folder and restart the server", ex);
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
}