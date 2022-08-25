package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.DefaultGenerator;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.generator.data.AreaData;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.schematic.RotationAngle;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.vector.Vector2D;
import dev.efnilite.vilib.vector.Vector3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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

    public synchronized Vector2D generate(@NotNull ParkourPlayer player, @Nullable ParkourGenerator generator, boolean generateIsland) {
        if (getPoint(player) != null) { // player already has assigned point
            return getPoint(player);
        }

        if (cachedPoints.size() > 0) { // check for leftover spots, if none are left just generate a new one
            Vector2D cachedPoint = cachedPoints.poll(); // get top result

            if (cachedPoint == null) { // if cachedPoint is somehow still null after checking size (to remove @NotNull warning)
                generate(player, generator, generateIsland);
                return getPoint(player);
            }

            activePoints.put(player, cachedPoint);
            if (generator != null) {
                player.setGenerator(generator);
            }
            if (generateIsland) {
                createIsland(player, cachedPoint);
            }
            return cachedPoint;
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
            return point;
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

        if (player.getGenerator() instanceof DefaultGenerator generator) {
            AreaData data = generator.getData();

            if (data != null) {
                for (Block block : data.blocks()) {
                    block.setType(Material.AIR, false);
                }
            }
        }

        Session session = player.getSession();
        session.removePlayers(player);
        session.unregister();
    }

    private synchronized void createIsland(@NotNull ParkourPlayer pp, @NotNull Vector2D point) {
        World world = IP.getWorldHandler().getWorld();

        Location spawn = getEstimatedCenter(point, Option.BORDER_SIZE).toLocation(world).clone();

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

        // get zone
        pp.getGenerator().setZone(getZone(spawn));

        if (to != null && parkourBegin != null && pp.getGenerator() instanceof DefaultGenerator defaultGenerator) {
            defaultGenerator.setData(new AreaData(blocks));
            defaultGenerator.generateFirst(to.clone(), parkourBegin.clone());
        }

        // setup inventory, etc.
        setup(pp, to, true);
    }

    public void setup(ParkourPlayer pp, Location to, boolean runGenerator) {
        Player player = pp.player;

        if (to != null) {
            pp.teleport(to);
        }
        player.setGameMode(GameMode.ADVENTURE);

        // -= Inventory =-
        if (Option.INVENTORY_HANDLING) {
            Task.create(IP.getPlugin())
                    .delay(5)
                    .execute(() -> {
                        List<Item> items = new ArrayList<>();

                        player.getInventory().clear();

                        if (ParkourOption.PLAY.check(player)) {
                            items.add(0, Locales.getItem(pp.getLocale(), "play.item"));
                        }

                        if (ParkourOption.COMMUNITY.check(player)) {
                            items.add(1, Locales.getItem(pp.getLocale(), "community.item"));
                        }

                        if (ParkourOption.SETTINGS.check(player)) {
                            items.add(2, Locales.getItem(pp.getLocale(), "settings.item"));
                        }

                        if (ParkourOption.LOBBY.check(player)) {
                            items.add(3, Locales.getItem(pp.getLocale(), "lobby.item"));
                        }

                        items.add(4, IP.getConfiguration().getFromItemData(pp.getLocale(), "general.quit"));

                        List<Integer> slots = Util.getEvenlyDistributedSlots(items.size());
                        for (int i = 0; i < items.size(); i++) {
                            player.getInventory().setItem(slots.get(i), items.get(i).build());
                        }
                    })
                    .run();
        }

        if (!Option.INVENTORY_HANDLING) {
            pp.sendTranslated("customize-menu");
        }

        if (runGenerator) {
            pp.getGenerator().startTick();
        }
    }

    /**
     * Gets the {@link Selection} instance used as the playable zone, given a specific center location
     *
     * @param   center
     *          The center location
     *
     * @return the playable area
     */
    public Selection getZone(Location center) {
        double borderSize = Option.BORDER_SIZE;

        // get the min and max locations
        Location min = center.clone().subtract(borderSize / 2, 0, borderSize / 2);
        Location max = center.clone().add(borderSize / 2, 0, borderSize / 2);

        min.setY(Option.MIN_Y);
        max.setY(Option.MAX_Y);

        return new Selection(min, max);
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
        return new Vector3D(vector.x * size, (Option.MAX_Y + Option.MIN_Y) / 2.0, vector.y * size);
    }
}