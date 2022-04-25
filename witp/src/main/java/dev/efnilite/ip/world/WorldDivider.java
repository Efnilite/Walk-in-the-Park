package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.generator.AreaData;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.schematic.RotationAngle;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.SingleSession;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Version;
import dev.efnilite.vilib.vector.Vector2D;
import dev.efnilite.vilib.vector.Vector3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Divides the Parkour world into sections so there can be an infinite amount of players in 1 world.
 *
 * @author Efnilite
 */
public class WorldDivider {

    private final Schematic spawnIsland;

    private final int spawnYaw;
    private final int spawnPitch;
    private final Material playerSpawn;
    private final Material parkourSpawn;

    /**
     * Spaces which have been previously generated but now have no players, so instead of generating a new point
     * it just picks an old one without any players.
     */
    private final Queue<Vector2D> cachedPoints = new LinkedList<>(); // final to reduce chance of thread collision

    /**
     * Currently active points
     */
    private final HashMap<ParkourPlayer, Vector2D> activePoints = new HashMap<>();

    /**
     * New instance of the SubareaDivider
     *
     * Note: initiating another instance of this class might lead to serious problems
     */
    @SuppressWarnings("ConstantConditions")
    public WorldDivider() {
        FileConfiguration gen = IP.getConfiguration().getFile("generation");
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
    public @Nullable Vector2D getPoint(@NotNull ParkourPlayer player) {
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
            Vector2D cachedPoint = cachedPoints.poll(); // get top result

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
        } else {
            int size = activePoints.size();
            int[] coords = Util.spiralAt(size);
            Vector2D point = new Vector2D(coords[0], coords[1]);

            activePoints.put(player, point);
            if (generator != null) {
                player.setGenerator(generator);
            }
            if (generateIsland) {
                createIsland(player, point);
            }
        }
    }

    /**
     * Removes a player from the registry
     * If you're using the API, please use {@link ParkourPlayer#register(Player)} instead!
     *
     * @param   player
     *          The player
     */
    public synchronized void leave(@NotNull ParkourPlayer player) {
        Vector2D point = getPoint(player);

        cachedPoints.add(point);
        activePoints.remove(player);

        if (player.getGenerator() instanceof DefaultGenerator) {
            AreaData data = ((DefaultGenerator) player.getGenerator()).getData();

            if (data != null) {
                for (Chunk spawnChunk : data.spawnChunks()) {
                    spawnChunk.setForceLoaded(false);
                }
                for (Block block : data.blocks()) {
                    block.setType(Material.AIR, false);
                }
            }
        }

        Session session = player.getSession();
        session.removePlayers(player);
        session.unregister();
    }

    // https://math.stackexchange.com/a/163101
    public List<Chunk> getChunksAround(Chunk base, int radius) {
        World world = IP.getWorldHandler().getWorld();

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

    private synchronized void createIsland(@NotNull ParkourPlayer pp, @NotNull Vector2D point) {
        World world = IP.getWorldHandler().getWorld();

        double borderSize = Option.BORDER_SIZE.get();

        Location spawn = getEstimatedCenter(point, borderSize).toLocation(world).clone();

        List<Chunk> chunks = new ArrayList<>();
        try {
            chunks = getChunksAround(spawn.getChunk(), 1);
            if (Version.isHigherOrEqual(Version.V1_13)) {
                for (Chunk chunk : chunks) {
                    chunk.setForceLoaded(true);
                }
            }
        } catch (Throwable ignored) {} // ignored if chunks can't be requested

        // --- Schematic pasting ---
        Vector3D dimension = spawnIsland.getDimensions().toVector3D();
        spawn.setY(spawn.getY() - dimension.y);
        List<Block> blocks = spawnIsland.paste(spawn, RotationAngle.ANGLE_0);

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
            IP.logging().stack("Couldn't find the spawn of a player", "check your block types and schematics");
            blocks.forEach(b -> b.setType(Material.AIR, false));
            createIsland(pp, point);
        }
        if (!parkourDetected) {
            IP.logging().stack("Couldn't find the spawn of the parkour", "check your block types and schematics");
            blocks.forEach(b -> b.setType(Material.AIR, false));
            createIsland(pp, point);
        }

        // get the min and max locations
        Location min = spawn.clone().subtract(borderSize / 2, 0, borderSize / 2);
        Location max = spawn.clone().add(borderSize / 2, 0, borderSize / 2);

        min.setY(Option.MIN_Y.get());
        max.setY(Option.MAX_Y.get());

        // set the proper zone
        pp.getGenerator().setZone(new Selection(min, max));

        if (to != null && parkourBegin != null && pp.getGenerator() instanceof DefaultGenerator defaultGenerator) {

            defaultGenerator.setData(new AreaData(blocks, chunks));
            defaultGenerator.generateFirst(to.clone(), parkourBegin.clone());
        }

        // setup inventory, etc.
        setup(to, pp);
    }

    public void setupSession(ParkourPlayer pp) {
        // create session
        Session session = new SingleSession();
        session.addPlayers(pp);
        session.register();

        // set session id for player
        pp.setSessionId(session.getSessionId());
    }

    public void setup(Location to, ParkourPlayer pp) {
        Player player = pp.getPlayer();

        setupSession(pp);

        pp.teleport(to);

        // -= Inventory =-
        player.setGameMode(GameMode.ADVENTURE);
        if (Option.INVENTORY_HANDLING.get() && Option.OPTIONS_ENABLED.get()) {
            player.getInventory().clear();
            ItemStack mat = IP.getConfiguration().getFromItemData(pp.getLocale(), "general.menu").build();
            if (mat == null) {
                IP.logging().error("Material for options in config is null - defaulting to compass");
                player.getInventory().setItem(8, new Item(Material.COMPASS, "&c&l-= Options =-").build());
            } else {
                player.getInventory().setItem(8, mat);
            }
        }
        if (Option.INVENTORY_HANDLING.get() && Option.HOTBAR_QUIT_ITEM.get()) {
            ItemStack mat = IP.getConfiguration().getFromItemData(pp.getLocale(), "general.quit").build();
            if (mat == null) {
                IP.logging().error("Material for quitting in config is null - defaulting to barrier");
                player.getInventory().setItem(7, new Item(Material.BARRIER, "&c&l-= Quit =-").build());
            } else {
                player.getInventory().setItem(7, mat);
            }
        }

        if (!Option.INVENTORY_HANDLING.get()) {
            pp.sendTranslated("customize-menu");
        }
        pp.getGenerator().startTick();
    }

    /**
     * Gets the estimated center of an area
     *
     * @param   vector
     *          The position
     *
     * @param   borderSize
     *          The border size
     *
     * @return the vector in the middle
     */
    public Vector3D getEstimatedCenter(Vector2D vector, double borderSize) {
        int size = (int) borderSize;
        return new Vector3D(vector.x * size, 150, vector.y * size);
    }
}