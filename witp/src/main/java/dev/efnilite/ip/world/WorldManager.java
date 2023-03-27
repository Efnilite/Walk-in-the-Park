package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
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
    static void delete() {
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
    static World getWorld() {
        return Bukkit.getWorld(Option.WORLD_NAME);
    }

    /**
     * Returns the appropriate instance.
     *
     * @return The appropriate instance.
     */
    static WorldManager getInstance() {
        if (WorldManagerMV.MANAGER != null) {
            return new WorldManagerMV();
        } else {
            return new WorldManagerMC();
        }
    }
}