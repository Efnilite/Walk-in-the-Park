package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.ip.world.generation.VoidGenerator;
import dev.efnilite.vilib.util.Version;
import org.bukkit.*;

import java.io.File;

/**
 * Class for handling Parkour world generation/deletion, etc.
 */
public class WorldHandler {

    private World world;

    /**
     * Creates the parkour world with the name given in the config.
     */
    public void createWorld() {
        String name = Option.WORLD_NAME;
        World world = Bukkit.getWorld(name);

        if (!Option.DELETE_ON_RELOAD) {
            if (world == null) {
                create();
            }
            return;
        }

        if (world == null) { // on crash prevent loading twice
            deleteWorld();
            create();
        } else {
            this.world = world;
            IP.logging().warn("## ");
            IP.logging().warn("## Crash detected! If there are any blocks left in the Parkour world, reload your server.");
            IP.logging().warn("## ");
        }
    }

    private void create() {
        String name = Option.WORLD_NAME;

        if (IP.getMultiverseHook() != null) { // if multiverse isn't detected
            world = IP.getMultiverseHook().createWorld(name);
        } else {
            try {
                WorldCreator creator = new WorldCreator(name)
                        .generateStructures(false)
                        .type(WorldType.NORMAL)
                        .generator(VoidGenerator.getGenerator()) // to fix No keys in MapLayer etc.
                        .environment(World.Environment.NORMAL);

                world = Bukkit.createWorld(creator);
                if (world == null) {
                    IP.logging().stack("Error while trying to create the parkour world",
                            "delete the parkour world folder and restart the server");
                }
            } catch (Throwable throwable) {
                IP.logging().stack("Error while trying to create the parkour world",
                        "delete the parkour world folder and restart the server", throwable);
            }
        }

        setupSettings();
    }

    /**
     * Deletes the parkour world
     */
    public void deleteWorld() {
        if (!Option.DELETE_ON_RELOAD) {
            return;
        }

        String name = Option.WORLD_NAME;

        if (IP.getMultiverseHook() != null) {
            IP.getMultiverseHook().deleteWorld(name);
        } else {
            Bukkit.unloadWorld(name, false);
            File folder = new File(name);
            if (folder.exists() && folder.isDirectory()) {
                folder.delete();
            }
        }
    }

    // Sets up the settings for the world
    @SuppressWarnings("deprecation")
    private void setupSettings() {
        if (Version.isHigherOrEqual(Version.V1_13)) {
            world.setGameRule(GameRule.DO_FIRE_TICK, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_TILE_DROPS, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        } else {
            world.setGameRuleValue("doFireTick", "false");
            world.setGameRuleValue("doMobSpawning", "false");
            world.setGameRuleValue("doTileDrops", "false");
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setGameRuleValue("keepInventory", "true");
            world.setGameRuleValue("doWeatherCycle", "false");
            world.setGameRuleValue("logAdminCommands", "false");
            world.setGameRuleValue("announceAdvancements", "false");
        }

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setWeatherDuration(1000);
        world.setAutoSave(false);
    }

    /**
     * Gets the WITP Bukkit world.
     *
     * @return the Bukkit world wherein WITP is currently active.
     */
    public World getWorld() {
        if (world == null) {
            world = Bukkit.getWorld(Option.WORLD_NAME);
            if (world == null) {
                IP.logging().stack("World is null", "delete the parkour world folder and restart the server");
            }
        }
        return world;
    }
}