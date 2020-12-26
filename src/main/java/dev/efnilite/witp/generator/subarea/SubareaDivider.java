package dev.efnilite.witp.generator.subarea;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.VoidGenerator;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Divides the empty world into sections so there can be an infinite amount of players in 1 world/**
 *
 * Important notice: tempering with details in this class could result in complete malfunction of code since
 * this class has been meticulously made using a lot of cross-references. Same goes for
 * {@link dev.efnilite.witp.generator.ParkourGenerator}.
 *
 * @author Efnilite
 */
public class SubareaDivider {

    private int amount = 0;
    private int layer = 0;
    private double borderSize;
    private SubareaPoint current;
    private World world;
    private File spawnIsland;
    private Material villagerSpawn;
    private Material playerSpawn;
    /**
     * The SubareaPoints available in the current layer
     */
    private List<SubareaPoint> possibleInLayer;
    private HashMap<SubareaPoint, ParkourPlayer> collection;

    public SubareaDivider() {
        FileConfiguration config = WITP.getConfiguration().getFile("config");
        String worldName = config.getString("world");
        if (worldName == null) {
            Verbose.error("Name of world is null in config");
            return;
        }
        this.world = Bukkit.getWorld(worldName);
        if (world == null) {
            Verbose.info("World " + worldName + " doesn't exist! Creating one now...");
            this.world = createWorld(worldName);
        }
        FileConfiguration gen = WITP.getConfiguration().getFile("generation");
        this.villagerSpawn = Material.getMaterial(gen.getString("advanced.island-villager-spawn-block").toUpperCase());
        this.playerSpawn = Material.getMaterial(gen.getString("advanced.island-player-spawn-block").toUpperCase());
        this.borderSize = gen.getDouble("advanced.border-size");
        this.current = new SubareaPoint(0, 0);
        this.spawnIsland = new File(WITP.getInstance().getDataFolder() + "/structures/spawn-island.nbt");
        this.collection = new HashMap<>();
        this.possibleInLayer = new ArrayList<>();
    }

    /**
     * Gets the point where the player is at
     *
     * @param   player
     *          The player
     *
     * @return the point of the player
     */
    public @Nullable SubareaPoint getPoint(@NotNull ParkourPlayer player) {
        for (SubareaPoint point : collection.keySet()) {
            if (collection.get(point) == player) {
                return point;
            }
        }
        return null;
    }

    /**
     * Generates the next block
     *
     * @param   player
     *          The player of who the generator belongs to
     */
    public void generate(@NotNull ParkourPlayer player) {
        amount++;
        int copy = amount - 1;

        if (copy % 8 == 0) { // every new layer has +8 area points
            createIsland(player);
            collection.put(current, player);
            current = current.zero();
            layer++;

            fetchPossibleInLayer();
        } else {

        }
    }

    private void fetchPossibleInLayer() {
        SubareaPoint corner1 = new SubareaPoint(layer, layer); // layer 2 has an offset of 2, so it would be (2,2)
        SubareaPoint corner2 = new SubareaPoint(layer, -layer); // (2,-2)
        SubareaPoint corner3 = new SubareaPoint(-layer, layer); // (-2,2)
        SubareaPoint corner4 = new SubareaPoint(-layer, -layer); // (-2,-2)

        possibleInLayer.clear();

        List<SubareaPoint> loop = new ArrayList<>();
        loop.addAll(corner1.getInBetween(corner2));
        loop.addAll(corner1.getInBetween(corner3));
        loop.addAll(corner4.getInBetween(corner2));
        loop.addAll(corner4.getInBetween(corner3));

        loop1:
        for (SubareaPoint point : loop) { // removes duplicates
            for (SubareaPoint other : possibleInLayer) {
                if (other.equals(point)) {
                    continue loop1;
                }
            }
            possibleInLayer.add(point);
        }
    }

    private @Nullable World createWorld(String name) {
        WorldCreator creator = new WorldCreator(name).generateStructures(false).hardcore(false)
                .generator(new VoidGenerator()).environment(World.Environment.NORMAL);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            Verbose.error("Error while trying to create the parkour world - please restart and delete the folder");
            return null;
        }
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setAutoSave(false);
        world.save();
        return world;
    }

    private void setBorder(@NotNull ParkourPlayer player) {
        SubareaPoint point = getPoint(player);
        if (point == null) {
            Verbose.error("Error while trying to get the current SubareaPoint of player " + player.getPlayer().getName());
            return;
        }
        Vector estimated = point.getEstimatedCenter(borderSize);
        WITP.getVersionManager().setWorldBorder(player.getPlayer(), estimated, borderSize);
    }

    private void createIsland(ParkourPlayer pp) {
        Player player = pp.getPlayer();
        Location spawn = current.getEstimatedCenter(borderSize).toLocation(world).clone();

        Vector dimension = WITP.getVersionManager().getDimensions(spawnIsland, spawn);
        spawn.setY(spawn.getY() - dimension.getY());
        WITP.getVersionManager().pasteStructure(spawnIsland, spawn);
        Location min = spawn.clone();
        min.setX(min.getX() - (dimension.getX() / 2.0));
        min.setZ(min.getZ() - (dimension.getZ() / 2.0));

        List<Location> blocks = Util.getBlocks(min, min.clone().add(dimension));
        Location to = null;
        boolean playerDetected = false;
        boolean villagerDetected = false;
        for (Location block : blocks) {
            Material type = block.getBlock().getType();
            if (type == playerSpawn) {
                to = block.clone().add(0.5, 1, 0.5);
                player.teleport(to);
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                playerDetected = true;
            }
        }
        if (!playerDetected) {
            Verbose.error("Couldn't find the spawn of a player - please check your block types and structures");
        }
        if (to != null) {
            pp.getGenerator().generateFirst(to, to.clone().subtract(0, 1, dimension.getZ() / 2.0));
        }
        BukkitRunnable delay = new BukkitRunnable() {
            @Override
            public void run() {
                setBorder(pp);
            }
        };
        Tasks.syncDelay(delay, 10 * 20);
    }
}
