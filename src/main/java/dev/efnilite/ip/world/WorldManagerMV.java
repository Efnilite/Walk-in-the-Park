package dev.efnilite.ip.world;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.util.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;

/**
 * Multiverse world manager.
 */
public class WorldManagerMV implements WorldManager {

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

        MANAGER.addWorld(Option.WORLD_NAME, World.Environment.NORMAL, null, WorldType.NORMAL, false, VoidGenerator.getMultiverseGenerator());
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