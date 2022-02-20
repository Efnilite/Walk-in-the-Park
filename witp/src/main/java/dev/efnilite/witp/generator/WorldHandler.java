package dev.efnilite.witp.generator;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Version;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.VoidGenerator;
import dev.efnilite.witp.util.config.Option;
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
        if (!Option.DELETE_ON_RELOAD.get()) {
            return;
        }

        String name = Option.WORLD_NAME.get();

        if (WITP.getMultiverseHook() != null) { // if multiverse isn't detected
            world = WITP.getMultiverseHook().createWorld(name);
        } else {
            try {
                WorldCreator creator = new WorldCreator(name)
                        .generateStructures(false)
                        .type(WorldType.NORMAL)
                        .generator(new VoidGenerator()) // to fix No keys in MapLayer etc.
                        .environment(World.Environment.NORMAL);

                world = Bukkit.createWorld(creator);
                if (world == null) {
                    Logging.stack("Error while trying to create the parkour world", "Delete the witp world folder and restart!");
                }
            } catch (Throwable throwable) {
                Logging.stack("Error while trying to create the parkour world", "Delete the witp world folder and restart!", throwable);
            }
        }

        setupSettings();
    }

    /**
     * Deletes the parkour world
     */
    public void deleteWorld() {
        if (!Option.DELETE_ON_RELOAD.get()) {
            return;
        }

        String name = Option.WORLD_NAME.get();

        if (WITP.getMultiverseHook() != null) {
            WITP.getMultiverseHook().deleteWorld(name);
        } else {
            Bukkit.unloadWorld(name, false);
            File folder = new File(name);
            if (folder.exists() && folder.isDirectory()) {
                folder.delete();
            }
        }
        Logging.verbose("Deleted world " + name);
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
        return world;
    }
}