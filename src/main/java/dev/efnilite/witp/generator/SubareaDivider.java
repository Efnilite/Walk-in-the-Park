package dev.efnilite.witp.generator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.VoidGenerator;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public class SubareaDivider {

    private int amount = 0;
    private int layer = 0;
    private double borderSize;
    private SubareaPair current;
    private World world;
    private File spawnIsland;
    private Material villagerSpawn;
    private Material playerSpawn;
    private HashMap<SubareaPair, ParkourPlayer> collection;

    public SubareaDivider() {
        FileConfiguration config = WITP.getConfiguration().getFile("config");
        String worldName = config.getString("world");
        if (worldName == null) {
            Verbose.error("Name of world is null in config");
            return;
        }
        world = Bukkit.getWorld(worldName);
        if (world == null) {
            Verbose.info("World " + worldName + " doesn't exist! Creating one now...");
            world = createWorld(worldName);
        }
        FileConfiguration gen = WITP.getConfiguration().getFile("generation");
        villagerSpawn = Material.getMaterial(gen.getString("advanced.island-villager-spawn-block").toUpperCase());
        playerSpawn = Material.getMaterial(gen.getString("advanced.island-player-spawn-block").toUpperCase());
        current = new SubareaPair(0, 0);
        spawnIsland = new File(WITP.getInstance().getDataFolder() + "/structures/spawn_island.nbt");
        borderSize = config.getDouble("advanced.border-size");
        collection = new HashMap<>();
    }

    public void generate(ParkourPlayer player) {
        amount++;
        int copy = amount - 1;

        if (copy % 8 == 0) {
            createIsland(player.getPlayer());
            collection.put(current, player);
            current = current.zero();
            layer++;
            current.x = layer;
        } else {
            SubareaPair next = current;
            next.x++;
            if (collection.get(next) != null) {

            }
        }
    }

    private World createWorld(String name) {
        WorldCreator creator = new WorldCreator(name).generateStructures(false).hardcore(false)
                .generator(new VoidGenerator()).environment(World.Environment.NORMAL);
        return Bukkit.createWorld(creator);
    }

    private void setBorder(Player player) {
        PacketContainer containerCenter = new PacketContainer(PacketType.Play.Server.WORLD_BORDER);
        PacketContainer containerSize = new PacketContainer(PacketType.Play.Server.WORLD_BORDER);

        containerSize.getWorldBorderActions().write(0, EnumWrappers.WorldBorderAction.SET_SIZE);
        containerCenter.getWorldBorderActions().write(0, EnumWrappers.WorldBorderAction.SET_CENTER);

        containerSize.getDoubles().write(0, borderSize);

        Vector estimated = current.getEstimatedCenter(borderSize);
        containerCenter.getDoubles().write(0, estimated.getX());
        containerCenter.getDoubles().write(1, estimated.getZ());

        try {
            WITP.getProtocolManager().sendServerPacket(player, containerCenter);
            WITP.getProtocolManager().sendServerPacket(player, containerSize);
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            Verbose.error("There was an error while trying to set the world border of player " + player.getName());
        }
    }

    private void createIsland(Player player) {
        Location spawn = current.getEstimatedCenter(borderSize).toLocation(world).clone();

        Vector dimension = WITP.getStructureManager().dimensions(spawnIsland, spawn);
        spawn.setY(spawn.getY() - dimension.getY());
        WITP.getStructureManager().paste(spawnIsland, spawn);
        Location min = spawn.clone();
        min.setX(min.getX() - (dimension.getX() / 2.0));
        min.setZ(min.getZ() - (dimension.getZ() / 2.0));

        List<Location> blocks = Util.getBlocks(min, min.clone().add(dimension));
        boolean playerDetected = false;
        boolean villagerDetected = false;
        for (Location block : blocks) {
            Material type = block.getBlock().getType();
            System.out.println(type.name() + " // " + playerSpawn.name());
            if (type == playerSpawn) {
                player.teleport(block.clone().add(0.5, 1, 0.5));
                playerDetected = true;
            } else if (type == villagerSpawn && !villagerDetected) {
                // spawn villager
                villagerDetected = true;
            }
        }
        if (!playerDetected) {
            Verbose.error("Couldn't find the spawn of a player - please check your block types and structures");
            player.teleport(spawn);
        }
        if (!villagerDetected) {
            Verbose.error("Couldn't find the spawn of the villager - please check your block types and structures");
        }
        setBorder(player);
    }
}
