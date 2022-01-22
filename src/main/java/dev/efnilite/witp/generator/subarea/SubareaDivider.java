package dev.efnilite.witp.generator.subarea;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.schematic.RotationAngle;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.Vector3D;
import dev.efnilite.witp.util.Logging;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Version;
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
import java.util.*;

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

    private World world;
    private Schematic spawnIsland;

    private int spawnYaw;
    private int spawnPitch;
    private Material playerSpawn;
    private Material parkourSpawn;

    /**
     * Spaces which have been previously generated but now have no players, so instead of generating a new point
     * it just picks an old one without any players.
     */
    private volatile Queue<SubareaPoint> cachedPoints = new LinkedList<>(); // volatile to reduce chance of thread collision
    private volatile HashMap<ParkourPlayer, SubareaPoint> activePoints = new HashMap<>();

    /**
     * New instance of the SubareaDivider
     *
     * Note: initiating another instance of this class might lead to serious problems
     */
    @SuppressWarnings("ConstantConditions")
    public SubareaDivider() {
        Logging.verbose("Initializing SubareaDivider");
        FileConfiguration config = WITP.getConfiguration().getFile("config");
        String worldName = config.getString("world.name");
        if (worldName == null) {
            Logging.error("Name of world is null in config");
            return;
        }
        if (WITP.getMultiverseHook() == null) {
            Bukkit.unloadWorld(worldName, false);
            File folder = new File(worldName);
            if (folder.exists() && folder.isDirectory()) {
                folder.delete();
                Logging.verbose("Deleted world " + worldName);
            }
        } else {
            WITP.getMultiverseHook().deleteWorld(worldName);
            Logging.verbose("Deleted world " + worldName);
        }
        this.world = createWorld(worldName);
        FileConfiguration gen = WITP.getConfiguration().getFile("generation");
        this.spawnYaw = gen.getInt("advanced.island.spawn.yaw");
        this.spawnPitch = gen.getInt("advanced.island.spawn.pitch");
        this.playerSpawn = Material.getMaterial(gen.getString("advanced.island.spawn.player-block").toUpperCase());
        this.parkourSpawn = Material.getMaterial(gen.getString("advanced.island.parkour.begin-block").toUpperCase());

        this.spawnIsland = new Schematic().file("spawn-island.witp");
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
        for (ParkourPlayer loopPlayer : activePoints.keySet()) {
            if (loopPlayer == player) {
                return activePoints.get(loopPlayer);
            }
        }
        return null;
    }

    /**
     * Generates an area for the player with no set generator. This will be set to the default DefaultGenerator.
     *
     * @param   player
     *          The player
     */
    public synchronized void generate(@NotNull ParkourPlayer player) {
        this.generate(player, null, true);
    }

    public synchronized void generate(@NotNull ParkourPlayer player, @Nullable ParkourGenerator generator, boolean generateIsland) {
        if (getPoint(player) != null) { // player already has assigned point
            return;
        }

        if (cachedPoints.size() > 0) { // check for leftover spots, if none are left just generate a new one
            SubareaPoint cachedPoint = cachedPoints.poll(); // get top result

            if (cachedPoint == null) { // if cachedPoint is somehow still null after checking size (to remove @NotNull warning)
                generate(player, generator, generateIsland);
                return;
            }

            activePoints.put(player, cachedPoint);
            if (generator != null) {
                player.setGenerator(generator);
            }
            if (generateIsland) {
                createIsland(player, cachedPoint);
            }

            Logging.verbose("Cached point divided to " + player.getPlayer().getName() + " at " + cachedPoint);
        } else {
            int size = activePoints.size();
            int[] coords = Util.spiralAt(size);
            SubareaPoint point = new SubareaPoint(coords[0], coords[1]);

            activePoints.put(player, point);
            if (generator != null) {
                player.setGenerator(generator);
            }
            if (generateIsland) {
                createIsland(player, point);
            }

            Logging.verbose("New point divided to " + player.getPlayer().getName() + " at " + point);
        }
    }

    /**
     * Removes a player from the registry
     * If you're using the API, please use {@link WITPAPI#unregisterPlayer(ParkourPlayer, boolean)}} instead!
     *
     * @param   player
     *          The player
     */
    public synchronized void leave(@NotNull ParkourPlayer player) {
        SubareaPoint point = getPoint(player);

        cachedPoints.add(point);
        activePoints.remove(player);
        Logging.verbose("Cached point " + point);

        SubareaPoint.Data data = player.getGenerator().data;
        for (Chunk spawnChunk : data.spawnChunks) {
            spawnChunk.setForceLoaded(false);
        }
        for (Block block : data.blocks) {
            block.setType(Material.AIR, false);
        }
    }

    // https://math.stackexchange.com/a/163101
    public List<Chunk> getChunksAround(Chunk base, int radius) {
        int lastOfRadius = 2 * radius + 1;
        int baseX = base.getX();
        int baseZ = base.getZ();

        List<Chunk> chunks = new ArrayList<>();
        int amount = lastOfRadius * lastOfRadius;
        for (int i = 0; i < amount; i++) {
            int[] coords = Util.spiralAt(i);
            int x = coords[0];
            int z = coords[1];

            x += baseX;
            z += baseZ;

            chunks.add(world.getChunkAt(x, z));
        }
        return chunks;
    }

    @SuppressWarnings("deprecation") // for setGameRuleValue
    private @Nullable World createWorld(String name) {
        World world;
        if (WITP.getMultiverseHook() == null) { // if multiverse isn't detected
            try {
                WorldCreator creator = new WorldCreator(name)
                        .generateStructures(false)
                        .type(WorldType.NORMAL)
                        .generator(new VoidGenerator()) // to fix No keys in MapLayer etc..
                        .environment(World.Environment.NORMAL);

                world = Bukkit.createWorld(creator);
                if (world == null) {
                    Logging.stack("Error while trying to create the parkour world", "Delete the witp world forder and restart!", null);
                    return null;
                }
            } catch (Throwable throwable) {
                Logging.stack("Error while trying to create the parkour world", "Delete the witp world forder and restart!", throwable);
                return null;
            }
        } else { // if multiverse is detected
            world = WITP.getMultiverseHook().createWorld(name);
        }
        Logging.verbose("Created world " + name);

        // -= World gamerules & options =-
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

        world.setKeepSpawnInMemory(false);

        return world;
    }

    private synchronized void createIsland(@NotNull ParkourPlayer pp, @NotNull SubareaPoint point) {
        Location spawn = point.getEstimatedCenter(Option.BORDER_SIZE.get()).toLocation(world).clone();
        List<Chunk> chunks = new ArrayList<>();
        try {
            chunks = getChunksAround(spawn.getChunk(), 1);
            if (Version.isHigherOrEqual(Version.V1_13)) {
                for (Chunk chunk : chunks) {
                    chunk.setForceLoaded(true);
                }
            }
        } catch (Throwable ignored) {} // ignored if chunks cant be requested

        // --- Schematic pasting ---
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
                parkourBegin = block.getLocation().clone();
                block.setType(Material.AIR);
                parkourDetected = true;
            }
        }
        if (!playerDetected) {
            Logging.stack("Couldn't find the spawn of a player", "Please check your block types and schematics", null);
            blocks.forEach(b -> b.setType(Material.AIR, false));
            createIsland(pp, point);
        }
        if (!parkourDetected) {
            Logging.stack("Couldn't find the spawn of the parkour", "Please check your block types and schematics", null);
            blocks.forEach(b -> b.setType(Material.AIR, false));
            createIsland(pp, point);
        }

        pp.getGenerator().data = new SubareaPoint.Data(blocks, chunks);
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
        if (Option.INVENTORY_HANDLING.get() && Option.OPTIONS_ENABLED.get()) {
            player.getInventory().clear();
            ItemStack mat = WITP.getConfiguration().getFromItemData(pp.locale, "general.menu");
            if (mat == null) {
                Logging.error("Material for options in config is null - defaulting to compass");
                player.getInventory().setItem(8, new ItemBuilder(Material.COMPASS, "&c&l-= Options =-").build());
            } else {
                player.getInventory().setItem(8, mat);
            }
        }
        if (Option.INVENTORY_HANDLING.get() && Option.HOTBAR_QUIT_ITEM.get()) {
            ItemStack mat = WITP.getConfiguration().getFromItemData(pp.locale, "general.quit");
            if (mat == null) {
                Logging.error("Material for quitting in config is null - defaulting to barrier");
                player.getInventory().setItem(7, new ItemBuilder(Material.BARRIER, "&c&l-= Quit =-").build());
            } else {
                player.getInventory().setItem(7, mat);
            }
        }

        if (!Option.INVENTORY_HANDLING.get()) {
            pp.sendTranslated("customize-menu");
        }
        pp.getGenerator().startTick();

        // Make sure the player is in the correct world
        // Used to be a problem, don't know if it still is, too scared to remove it now :)
        Tasks.syncDelay(() -> {
            if (!player.getWorld().getUID().equals(world.getUID())) {
                player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }, 10);
    }

    public World getWorld() {
        return world;
    }
}