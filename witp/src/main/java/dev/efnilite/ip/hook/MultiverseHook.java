package dev.efnilite.ip.hook;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import dev.efnilite.ip.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.jetbrains.annotations.ApiStatus;

/**
 * Class for hooking with Multiverse, to avoid default worlds generating when users have MV installed.
 */
@ApiStatus.Internal
public class MultiverseHook {

    private static MVWorldManager manager;

    static {
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core != null) {
            manager = core.getMVWorldManager();
        }
    }

    public static boolean isActive() {
        return manager != null;
    }

    /**
     * Deletes the world
     *
     * @param   worldName
     *          The name of the world
     */
    public static void deleteWorld(String worldName) {
        if (manager == null) {
            return;
        }

        manager.deleteWorld(worldName, false); // deleteFromConfig
    }

    /**
     * Creates a world using Multiverse
     *
     * @param   worldName
     *          The name of the world
     *
     * @return the Bukkit version of this world
     */
    public static World createWorld(String worldName) {
        if (manager == null) {
            return null;
        }

        manager.addWorld(worldName, World.Environment.NORMAL, null, WorldType.FLAT, false, VoidGenerator.getMultiverseGenerator());
        MultiverseWorld world = manager.getMVWorld(worldName);

        // -= Optimizations to reduce memory usage =-
        world.setAllowAnimalSpawn(false);
        world.setAllowMonsterSpawn(false);
        world.setKeepSpawnInMemory(false);

        return world.getCBWorld();
    }
}