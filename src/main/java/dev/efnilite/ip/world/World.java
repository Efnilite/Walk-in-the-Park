package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.util.VoidGenerator;
import org.bukkit.*;

import java.io.File;

public class World {

    private static String name;

    private static org.bukkit.World world;

    /**
     * Creates the world.
     */
    public static void create() {
        name = Config.CONFIG.getString("world.name");

        IP.log("Creating world %s".formatted(name));

        if (IP.getMv() != null) {
            IP.getMv().addWorld(name, org.bukkit.World.Environment.NORMAL, null, WorldType.NORMAL,
                    false, VoidGenerator.getMultiverseGenerator());
            world = IP.getMv().getMVWorld(name).getCBWorld();
        } else {
            world = new WorldCreator(name)
                    .generator(VoidGenerator.getGenerator())
                    .generateStructures(false)
                    .type(WorldType.NORMAL)
                    .createWorld();
        }

        setup();
    }

    /**
     * Sets all world settings.
     */
    private static void setup() {
        IP.log("Setting up rules for world %s".formatted(name));

        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10000000.0);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setTime(10000);
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
    }

    /**
     * Deletes the world.
     */
    public static void delete() {
        IP.log("Deleting world %s".formatted(name));

        var file = new File(name);

        if (!file.exists()) {
            return;
        }
        IP.log("Unloading world %s".formatted(name));

        if (IP.getMv() != null) {
            IP.getMv().deleteWorld(Option.WORLD_NAME, false);
            return;
        }

        Bukkit.unloadWorld(name, false);

        try {
            deleteRecursive(file);
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to reset world", ex);
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null) return;

        if (fileOrDirectory.isDirectory()) {
            for (var child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    /**
     * @return The world name.
     */
    public static String getName() {
        return name;
    }

    /**
     * @return The world.
     */
    public static org.bukkit.World getWorld() {
        return world;
    }
}
