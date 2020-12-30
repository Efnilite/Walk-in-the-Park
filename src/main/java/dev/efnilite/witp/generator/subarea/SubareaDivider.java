package dev.efnilite.witp.generator.subarea;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.VoidGenerator;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.*;
import org.bukkit.block.Block;
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
    private SubareaPoint current;
    private World world;
    private File spawnIsland;

    private int spawnYaw;
    private int spawnPitch;
    private Material playerSpawn;
    private Material parkourSpawn;
    private Vector heading;

    /**
     * The SubareaPoints available in the current layer
     */
    private List<SubareaPoint> possibleInLayer;
    /**
     * Open spaces which may open up if a player leaves
     */
    private List<SubareaPoint> openSpaces;
    private HashMap<SubareaPoint, ParkourPlayer> collection;

    /**
     * New instance of the SubareaDivider
     *
     * Note: initiating another instance of this class might lead to serious problems
     */
    @SuppressWarnings("ConstantConditions")
    public SubareaDivider() {
        FileConfiguration config = WITP.getConfiguration().getFile("config");
        String worldName = config.getString("world");
        if (worldName == null) {
            Verbose.error("Name of world is null in config");
            return;
        }
        File folder = new File(worldName);
        if (folder.exists() && folder.isDirectory()) {
            folder.delete();
        }
        this.world = createWorld(worldName);
        FileConfiguration gen = WITP.getConfiguration().getFile("generation");
        this.spawnYaw = gen.getInt("advanced.island.spawn.yaw");
        this.spawnPitch = gen.getInt("advanced.island.spawn.pitch");
        this.playerSpawn = Material.getMaterial(gen.getString("advanced.island.spawn.player-block").toUpperCase());
        this.parkourSpawn = Material.getMaterial(gen.getString("advanced.island.parkour.begin-block").toUpperCase());
        this.heading = Util.getDirection(gen.getString("advanced.island.parkour.heading"));

        this.current = new SubareaPoint(0, 0);
        this.spawnIsland = new File(WITP.getInstance().getDataFolder() + "/structures/spawn-island.nbt");
        this.collection = new HashMap<>();
        this.openSpaces = new ArrayList<>();
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
     * Generates the next playable area
     *
     * @param   player
     *          The player of who the generator belongs to
     */
    public synchronized void generate(@NotNull ParkourPlayer player) {
        if (getPoint(player) == null) {
            amount++;
            int copy = amount - 1;

            if (openSpaces.size() > 0) {
                SubareaPoint last = openSpaces.get(openSpaces.size() - 1);
                createIsland(player, last);
                openSpaces.remove(last);
                return;
            }
            if (copy % 8 == 0) { // every new layer has +8 area points

                createIsland(player, current);
                current = current.zero();
                layer++;

                fetchPossibleInLayer();
            } else {
                if (possibleInLayer.size() == 0){
                    fetchPossibleInLayer();
                }
                SubareaPoint point = possibleInLayer.get(0);
                if (point == null) {
                    fetchPossibleInLayer();
                    point = possibleInLayer.get(0);
                }

                current = point;
                possibleInLayer.remove(point);
                createIsland(player, current);
            }
        }
    }

    /**
     * Removes a player from the registry
     * If you're using the API, please use {@link dev.efnilite.witp.WITPAPI#unregisterPlayer(ParkourPlayer)} instead!
     *
     * @param   player
     *          The player
     */
    public void leave(@NotNull ParkourPlayer player) {
        SubareaPoint point = getPoint(player);
        collection.remove(point);
        openSpaces.add(point);
        for (Block block : player.getGenerator().data.blocks) {
            block.setType(Material.AIR);
        }
    }

    // gets all possible points in a square in the current layer
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
        WorldCreator creator = new WorldCreator(name).generateStructures(false).hardcore(false).type(WorldType.FLAT)
                .generator(new VoidGenerator()).environment(World.Environment.NORMAL);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            Verbose.error("Error while trying to create the parkour world - please restart and delete the folder");
            return null;
        }
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setClearWeatherDuration(1000);
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
        int size = (int) player.getGenerator().borderOffset * 2;
        Vector estimated = point.getEstimatedCenter(size);
        WITP.getVersionManager().setWorldBorder(player.getPlayer(), estimated, size);
    }

    private void createIsland(ParkourPlayer pp, SubareaPoint point) {
        Player player = pp.getPlayer();
        collection.put(point, pp);
        Location spawn = point.getEstimatedCenter((int) pp.getGenerator().borderOffset * 2).toLocation(world).clone();

        Vector dimension = WITP.getVersionManager().getDimensions(spawnIsland, spawn);
        spawn.setY(spawn.getY() - dimension.getY());
        WITP.getVersionManager().pasteStructure(spawnIsland, spawn);
        Location min = spawn.clone();
        min.setX(min.getX() - (dimension.getX() / 2.0));
        min.setZ(min.getZ() - (dimension.getZ() / 2.0));

        List<Block> blocks = Util.getBlocks(min, min.clone().add(dimension));
        pp.getGenerator().data = new SubareaPoint.Data(blocks);
        pp.getGenerator().heading = heading.clone();
        Location to = null;
        Location parkourBegin = null;
        boolean playerDetected = false;
        boolean parkourDetected = false;
        for (Block block : blocks) {
            if (playerDetected && parkourDetected) {
                break;
            }
            Material type = block.getType();
            if (type == playerSpawn && !playerDetected) {
                to = block.getLocation().clone().add(0.5, 0, 0.5);
                to.setPitch(spawnPitch);
                to.setYaw(spawnYaw);
                player.teleport(to);
                block.setType(Material.AIR);
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                String mat = WITP.getConfiguration().getString("config", "options.item");
                if (mat == null) {
                    Verbose.error("Material for options in config is null - defaulting to compass");
                    mat = "COMPASS";
                }
                player.getInventory().setItem(8, new ItemBuilder(Material.getMaterial(mat.toUpperCase()), "&c&lOptions").build());
                playerDetected = true;
            } else if (type == parkourSpawn && !parkourDetected) {
                parkourBegin = block.getLocation().clone().add(heading.clone().multiply(-1)); // remove an extra block of jumping space
                block.setType(Material.AIR);
                parkourDetected = true;
            }
        }
        if (!playerDetected) {
            Verbose.error("Couldn't find the spawn of a player - please check your block types and structures");
        }
        if (!parkourDetected) {
            Verbose.error("Couldn't find the spawn of the parkour - please check your block types and structures");
        }
        if (to != null && parkourBegin != null) {
            pp.getGenerator().generateFirst(to.clone(), parkourBegin.clone());
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
