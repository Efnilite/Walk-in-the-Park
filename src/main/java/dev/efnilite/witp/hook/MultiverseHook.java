package dev.efnilite.witp.hook;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import dev.efnilite.witp.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;

/**
 * Class for hooking with Multiverse, to avoid default worlds generating when users have MV installed.
 */
public class MultiverseHook {

    private final MultiverseCore core;
    private final MVWorldManager manager;

    public MultiverseHook() {
        core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (core == null) {
            throw new IllegalStateException("Initialized without plugin");
        }
        manager = core.getMVWorldManager();
    }

    public void deleteWorld(String worldName) {
        manager.deleteWorld(worldName, false); // deleteFromConfig
    }

    public World createWorld(String worldName) {
        manager.addWorld(worldName, World.Environment.NORMAL, null, WorldType.FLAT, false, Util.getVoidGenerator());
        MultiverseWorld world = manager.getMVWorld(worldName);

        // -= Optimizations to reduce memory usage =-
        world.setAllowAnimalSpawn(false);
        world.setAllowMonsterSpawn(false);
        world.setKeepSpawnInMemory(false);

        return world.getCBWorld();
    }
}