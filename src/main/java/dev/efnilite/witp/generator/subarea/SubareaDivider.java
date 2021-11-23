package dev.efnilite.witp.generator.subarea;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.schematic.RotationAngle;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.Vector3D;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
    private Schematic spawnIsland;

    private int spawnYaw;
    private int spawnPitch;
    private Material playerSpawn;
    private Material parkourSpawn;

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
        Verbose.verbose("Initializing SubareaDivider");
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

        this.current = new SubareaPoint(0, 0);
        this.spawnIsland = new Schematic().file("spawn-island.witp");
        this.collection = new HashMap<>();
        this.openSpaces = new ArrayList<>();
        this.possibleInLayer = new ArrayList<>();
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

    public boolean isOccupied(SubareaPoint point) {
        return collection.get(point) != null; // if null not occupied, so return false
    }

    public synchronized void generate(@NotNull ParkourPlayer player, @NotNull ParkourGenerator generator) {
        generate(player, generator, true);
    }

    /**
     * Generates the next playable area
     *
     * @param   player
     *          The player of who the generator belongs to
     *
     * @param   generator
     *          The generator instance of the player
     *
     * @param   checkHistory
     *          Whether it should check if there are previously used places.
     */
    public synchronized void generate(@NotNull ParkourPlayer player, @NotNull ParkourGenerator generator, boolean checkHistory) {
        if (getPoint(player) == null) {
            if (checkHistory && !openSpaces.isEmpty()) { // spaces which were previously used
                SubareaPoint last = openSpaces.get(openSpaces.size() - 1);

                if (last == null) {
                    openSpaces.clear();
                    generate(player, generator, false);
                }

                if (isOccupied(last)) { // if it is being used
                    openSpaces.remove(last); // remove it from possible
                    amount--;
                    generate(player, generator, true);
                }

                amount++;
                createIsland(player, generator, last);
                openSpaces.remove(last);
                Verbose.verbose("Used Subarea divided to " + player.getPlayer().getName());
                return;
            }

            if (amount % 8 == 0) { // every new layer has +8 area points
                amount++;
                createIsland(player, generator, current);

                current = current.zero(); // reset
                layer++;

                fetchPossibleInLayer();
                Verbose.verbose("Layer increase in Divider");

            } else {
                if (possibleInLayer.isEmpty()) {
                    fetchPossibleInLayer();
                }

                SubareaPoint point = possibleInLayer.get(0);

                if (point == null) {
                    fetchPossibleInLayer();
                    generate(player, generator, false);
                }

                if (isOccupied(point)) {
                    possibleInLayer.remove(point);
                    amount--;
                    generate(player, generator, false);
                }

                current = point;
                possibleInLayer.remove(point);

                amount++;
                createIsland(player, generator, current);
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
        if (WITP.getMultiverseHook() == null) { // if multiverse isn't detected
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
            // -= Optimizations =-
            world.setKeepSpawnInMemory(false);

        } else { // if multiverse is detected
            world = WITP.getMultiverseHook().createWorld(name);
        }
        Verbose.verbose("Created world " + name);

        // -= World gamerules & options =-
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

    private void createIsland(@NotNull ParkourPlayer pp, ParkourGenerator generator, SubareaPoint point) {
        if (point == null) {
            generate(pp, generator, false);
            return;
        }
        collection.put(point, pp);
        Location spawn = point.getEstimatedCenter((int) Option.BORDER_SIZE).toLocation(world).clone();

        Vector3D dimension = spawnIsland.getDimensions().getDimensions();
        spawn.setY(spawn.getY() - dimension.y);
        List<Block> blocks = spawnIsland.paste(spawn, RotationAngle.ANGLE_0);
        Location min = spawn.clone();
        min.setX(min.getX() - (dimension.x / 2.0));
        min.setZ(min.getZ() - (dimension.z / 2.0));

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
                block.setType(Material.AIR);
                playerDetected = true;
            } else if (type == parkourSpawn && !parkourDetected) {
                parkourBegin = block.getLocation().clone().add(Util.getDirectionVector(Option.HEADING).multiply(-1).toBukkitVector()); // remove an extra block of jumping space
                block.setType(Material.AIR);
                parkourDetected = true;
            }
        }
        if (!playerDetected) {
            Verbose.error("Couldn't find the spawn of a player - please check your block types and schematics");
        }
        if (!parkourDetected) {
            Verbose.error("Couldn't find the spawn of the parkour - please check your block types and schematics");
        }

        if (pp.getGenerator() == null) {
            pp.setGenerator(new DefaultGenerator(pp));
        }

        pp.getGenerator().data = new SubareaPoint.Data(blocks);
        if (to != null && parkourBegin != null && pp.getGenerator() instanceof DefaultGenerator) {
            ((DefaultGenerator) pp.getGenerator()).generateFirst(to.clone(), parkourBegin.clone());
        }

        setup(to, pp);
    }

    public void setup(Location to, ParkourPlayer pp) {
        Player player = pp.getPlayer();

        pp.teleport(to);

        // -= Inventory =-
        player.setGameMode(GameMode.ADVENTURE);
        if (Option.INVENTORY_HANDLING && Option.OPTIONS_ENABLED) {
            player.getInventory().clear();
            ItemStack mat = WITP.getConfiguration().getFromItemData(pp.locale, "general.menu");
            if (mat == null) {
                Verbose.error("Material for options in config is null - defaulting to compass");
                player.getInventory().setItem(8, new ItemBuilder(Material.COMPASS, "&c&l-= Options =-").build());
            } else {
                player.getInventory().setItem(8, mat);
            }
        }

        if (!Option.INVENTORY_HANDLING) {
            pp.sendTranslated("customize-menu");
        }
        pp.getGenerator().start();

        // Make sure the player is in the correct world
        // Used to be a problem, don't know if it still is, too scared to remove it now :)
        Tasks.syncDelay(() -> {
            if (!player.getWorld().getUID().equals(world.getUID())) {
                player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }, 10);
    }
}
