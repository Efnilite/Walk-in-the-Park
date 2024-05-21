package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.vilib.util.VoidGenerator;
import org.bukkit.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class WorldManager {

    private static String name;
    private static World world;

    /**
     * Creates a new world and sets all according settings in it.
     */
    public static void create() {
        name = Config.CONFIG.getString("world.name");

        var world = Bukkit.getWorld(name);

        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        if (world != null) {
            IP.logging().warn("Crash detected! The parkour world loading twice is not usual behaviour. This only happens after a server crash.");
        }

        if (Config.CONFIG.getBoolean("world.delete-on-reload")) {
            deleteWorld();
        }

        createWorld();
        setup();
    }

    private static void createWorld() {
        IP.log("Creating Spigot world");

        try {
            WorldCreator creator = new WorldCreator(name)
                    .generateStructures(false)
                    .type(WorldType.NORMAL)
                    .generator(VoidGenerator.getGenerator()) // to fix No keys in MapLayer etc.
                    .environment(World.Environment.NORMAL);

            world = Bukkit.createWorld(creator);
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to create the parkour world", "delete the parkour world folder and restart the server", ex);
        }
    }

    private static void setup() {
        IP.log("Initializing world rules");

        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10_000_000);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setAutoSave(false);
    }

    /**
     * Deletes the world.
     */
    public static void delete() {
        if (!Config.CONFIG.getBoolean("world.delete-on-reload") || !Config.CONFIG.getBoolean("joining")) {
            return;
        }
        IP.log("Deleting world");

        getWorld().getPlayers().forEach(player -> player.kickPlayer("Server is restarting"));

        deleteWorld();
    }

    private static void deleteWorld() {
        IP.log("Deleting Spigot world");

        File file = new File(name);

        // world has already been deleted
        if (!file.exists()) {
            return;
        }

        Bukkit.unloadWorld(name, false);

        try (Stream<Path> files = Files.walk(file.toPath())) {
            files.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to delete the parkour world", ex);
        }
    }

    /**
     * @return the name of the parkour world.
     */
    public static String getName() {
        return name;
    }

    /**
     * @return the Bukkit world wherein IP is currently active.
     */
    public static World getWorld() {
        return world;
    }
}