package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Option;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;

public interface WorldManager {

    /**
     * Implementation for creating a world.
     *
     * @return The created world.
     */
    World createWorld();

    /**
     * Implementation for deleting a world.
     */
    void deleteWorld();

    /**
     * Creates a new world and sets all according settings in it.
     */
    static void create() {
        World world = getWorld();

        if (!Config.CONFIG.getBoolean("joining") || (!Config.CONFIG.getBoolean("world.delete-on-reload") && world != null)) {
            return;
        }

        if (world != null) {
            IP.logging().warn("Crash detected! The parkour world loading twice is not usual behaviour. This only happens after a server crash.");
        }

        WorldManager manager = getInstance();

        IP.log("Initializing world rules");

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

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10_000_000);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setAutoSave(false);
    }

    /**
     * Deletes the world.
     */
    static void delete() {
        if (!Config.CONFIG.getBoolean("world.delete-on-reload") || !Config.CONFIG.getBoolean("joining")) {
            return;
        }
        IP.log("Deleting world");

        getWorld().getPlayers().forEach(player -> player.kickPlayer("Server is restarting"));

        getInstance().deleteWorld();
    }

    /**
     * Gets the parkour world.
     *
     * @return the Bukkit world wherein IP is currently active.
     */
    static World getWorld() {
        return Bukkit.getWorld(Option.WORLD_NAME);
    }

    /**
     * Returns the appropriate instance.
     *
     * @return The appropriate instance.
     */
    static WorldManager getInstance() {
        return WorldManagerMV.MANAGER != null ? new WorldManagerMV() : new WorldManagerMC();
    }
}