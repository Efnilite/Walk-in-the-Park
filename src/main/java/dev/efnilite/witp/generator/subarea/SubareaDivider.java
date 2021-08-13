package dev.efnilite.witp.generator.subarea;

import com.onarandombox.MultiverseCore.utils.FileUtils;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.VoidGenerator;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.FileUtil;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Divides the empty world into sections so there can be an infinite amount of players in 1 world/**
 *
 * Important notice: tempering with details in this class could result in complete malfunction of code since
 * this class has been meticulously made using a lot of cross-references. Same goes for
 * {@link DefaultGenerator}.
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
        String worldName = config.getString("world.name");
        if (worldName == null) {
            Verbose.error("Name of world is null in config");
            return;
        }
        if (WITP.getMultiverseHook() == null) {
            Bukkit.unloadWorld(worldName, false);
            File folder = new File(worldName);
            if (folder.exists() && folder.isDirectory()) {
                folder.delete();
                Verbose.verbose("Deleted world " + worldName);
            }
        } else {
            WITP.getMultiverseHook().deleteWorld(worldName);
            Verbose.verbose("Deleted world " + worldName);
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

    public void setHeading(Vector heading) {
        this.heading = heading;
    }

    public World getWorld() {
        return world;
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
            if (collection.get(point).getPlayer().getUniqueId() == player.getPlayer().getUniqueId()) {
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
                if (collection.get(last) != null) {
                    Verbose.error("Used (cached) island is already assigned. Retrying without this island.");
                    openSpaces.remove(last);
                    amount--;
                    generate(player);
                }
                createIsland(player, last);
                openSpaces.remove(last);
                Verbose.verbose("Used Subarea divided to " + player.getPlayer().getName());
                return;
            }
            if (copy % 8 == 0) { // every new layer has +8 area points
                createIsland(player, current);
                current = current.zero();
                layer++;

                fetchPossibleInLayer();
                Verbose.verbose("Layer increase");
            } else {
                if (possibleInLayer.size() == 0){
                    fetchPossibleInLayer();
                }
                SubareaPoint point = possibleInLayer.get(0);
                if (point == null) {
                    fetchPossibleInLayer();
                    point = possibleInLayer.get(0);
                }
                if (point == null) {
                    Verbose.error("Playing space assignment has gone terribly wrong - adding to layer");
                    amount++;
                    fetchPossibleInLayer();
                    point = possibleInLayer.get(0);
                }
                if (collection.get(point) != null) {
                    Verbose.error("Island is already assigned. Retrying without this island.");
                    amount--;
                    possibleInLayer.remove(point);
                    generate(player);
                }

                current = point;
                possibleInLayer.remove(point);
                createIsland(player, current);
            }
            Verbose.verbose("New Subarea divided to " + player.getPlayer().getName());
        }
    }

    /**
     * Removes a player from the registry
     * If you're using the API, please use {@link WITPAPI#unregisterPlayer(ParkourPlayer, boolean)}} instead!
     *
     * @param   player
     *          The player
     */
    public void leave(@NotNull ParkourPlayer player) {
        SubareaPoint point = getPoint(player);
        collection.remove(point);
        openSpaces.add(point);
        for (Block block : player.getGenerator().data.blocks) {
            block.setType(Material.AIR, false);
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
            Verbose.verbose("Add point " + point.toString() + " to possible");
        }
    }

    private @Nullable World createWorld(String name) {
        World world;
        if (WITP.getMultiverseHook() == null) {
            WorldCreator creator = new WorldCreator(name)
                    .generateStructures(false)
                    .hardcore(false)
                    .type(WorldType.FLAT)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL);
            world = Bukkit.createWorld(creator);
            if (world == null) {
                Verbose.error("Error while trying to create the parkour world - please restart and delete the folder");
                return null;
            }
        } else {
            world = WITP.getMultiverseHook().createWorld(name);
        }
        Verbose.verbose("Created world " + name);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setWeatherDuration(1000);
        world.setAutoSave(false);
        return world;
    }

    public void setBorder(@NotNull ParkourUser player, @NotNull SubareaPoint point) {
        int size = (int) Option.BORDER_SIZE;
        Vector estimated = point.getEstimatedCenter(size);
        WITP.getVersionManager().setWorldBorder(player.getPlayer(), estimated, size);
    }

    private void createIsland(@NotNull ParkourPlayer pp, SubareaPoint point) {
        if (point == null) { // something has gone TERRIBLY WRONG, just in case
            Verbose.error("Point assignment after confirmation has gone terribly wrong, retrying..");
            generate(pp);
            return;
        }
        Player player = pp.getPlayer();
        collection.put(point, pp);
        Location spawn = point.getEstimatedCenter((int) Option.BORDER_SIZE).toLocation(world).clone();

        Vector dimension = WITP.getVersionManager().getDimensions(spawnIsland, spawn);
        spawn.setY(spawn.getY() - dimension.getY());
        WITP.getVersionManager().pasteStructure(spawnIsland, spawn);
        Location min = spawn.clone();
        min.setX(min.getX() - (dimension.getX() / 2.0));
        min.setZ(min.getZ() - (dimension.getZ() / 2.0));

        List<Block> blocks = Util.getBlocks(min, min.clone().add(dimension));

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
                to.setWorld(world);
                pp.teleport(to);
                block.setType(Material.AIR);
                player.setGameMode(GameMode.ADVENTURE);
                if (Option.INVENTORY_HANDLING) {
                    player.getInventory().clear();
                    ItemStack mat = WITP.getConfiguration().getFromItemData(pp.locale, "general.menu");
                    if (mat == null) {
                        Verbose.error("Material for options in config is null - defaulting to compass");
                        player.getInventory().setItem(8, new ItemBuilder(Material.COMPASS, "&c&lOptions").build());
                    } else {
                        player.getInventory().setItem(8, mat);
                    }
                }
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
        if (pp.getGenerator() == null) {
            pp.setGenerator(new DefaultGenerator(pp));
        }
        pp.getGenerator().data = new SubareaPoint.Data(blocks);
        pp.getGenerator().heading = heading.clone();
        if (to != null && parkourBegin != null) {
            if (pp.getGenerator() instanceof DefaultGenerator) {
                ((DefaultGenerator) pp.getGenerator()).generateFirst(to.clone(), parkourBegin.clone());
            }
        }

        if (!Option.INVENTORY_HANDLING) {
            pp.sendTranslated("customize-menu");
        }
        pp.getGenerator().start();

        // todo fix this check
        Location finalTo = to;
        Tasks.syncDelay(() -> {
            if (!player.getWorld().getUID().equals(world.getUID())) {
                player.teleport(finalTo, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }, 10);
        setBorder(pp, point);
    }

    public Vector getHeading() {
        return heading;
    }
}
