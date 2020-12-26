package dev.efnilite.witp.generator;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link dev.efnilite.witp.ParkourPlayer} has.<br>
 *
 * Important notice: tempering with details in this class could result in complete malfunction of code since
 * this class has been meticulously made using a lot of cross-references. Same goes for
 * {@link dev.efnilite.witp.generator.subarea.SubareaDivider}.
 *
 * @author Efnilite
 */
public class ParkourGenerator {

    /**
     * The score of the player
     */
    public int score;

    /**
     * The difficulty multiplier (goes up to 1.00)
     */
    public String time = "0ms";
    private Location lastSpawn;
    private Location lastPlayer;
    private Location previousSpawn;

    private Location playerSpawn;
    private Location blockSpawn;

    private final Stopwatch stopwatch;
    private final LinkedHashMap<String, Integer> buildLog;
    private final ParkourPlayer player;
    private final HashMap<Integer, Integer> defaultChances;
    private final HashMap<Integer, Integer> distanceChances;
    private final HashMap<Integer, Integer> heightChances;
    private final HashMap<Integer, Double> multiplierDecreases;

    public ParkourGenerator(ParkourPlayer player) {
        this.score = 0;
        this.player = player;
        this.lastSpawn = player.getPlayer().getLocation().clone();
        this.lastPlayer = lastSpawn.clone();
        this.defaultChances = new HashMap<>();
        this.distanceChances = new HashMap<>();
        this.heightChances = new HashMap<>();
        this.buildLog = new LinkedHashMap<>();
        this.stopwatch = new Stopwatch();
        this.multiplierDecreases = new HashMap<>();

        double multiplier = Configurable.MULTIPLIER;
        multiplierDecreases.put(1, (Configurable.MAXED_ONE_BLOCK - Configurable.NORMAL_ONE_BLOCK) / multiplier);
        multiplierDecreases.put(2, (Configurable.MAXED_TWO_BLOCK - Configurable.NORMAL_TWO_BLOCK) / multiplier);
        multiplierDecreases.put(3, (Configurable.MAXED_THREE_BLOCK - Configurable.NORMAL_THREE_BLOCK) / multiplier);
        multiplierDecreases.put(4, (Configurable.MAXED_FOUR_BLOCK - Configurable.NORMAL_FOUR_BLOCK) / multiplier);

        Tasks.syncRepeat(new BukkitRunnable() {
            @Override
            public void run() {
                Block current = player.getPlayer().getLocation().subtract(0, 1, 0).getBlock();
                if (lastPlayer.getY() - player.getPlayer().getLocation().getY() > 10) {
                    reset();
                    return;
                }
                String last = Util.toString(lastPlayer, false);
                if (current.getType() != Material.AIR) {
                    previousSpawn = lastPlayer.clone();
                    lastPlayer = current.getLocation();
                    if (buildLog.get(last) != null) {
                        if (!(Util.toString(previousSpawn, true).equals(Util.toString(lastPlayer, true)))) {
                            if (!stopwatch.hasStarted()) {
                                stopwatch.start();
                            }
                            score++;
                            player.updateScoreboard();
                            List<String> locations = new ArrayList<>(buildLog.keySet());
                            int lastIndex = locations.indexOf(last) + 1;
                            int size = locations.size();
                            for (int i = lastIndex; i < size; i++) {
                                Util.parseLocation(locations.get(i)).getBlock().setType(Material.AIR);
                            }
                        }

                        Integer latest = buildLog.get(last);
                        if (latest == null) {
                            return;
                        }
                        int difference = player.blockLead - latest;
                        if (difference > 0) {
                            generateNext(Math.abs(difference));
                        }
                    }
                }
                time = stopwatch.toString();
                player.getPlayer().setSaturation(20);
                player.updateScoreboard();
            }
        }, Configurable.GENERATOR_CHECK);
    }

    /**
     * Resets the parkour
     */
    public void reset() {
        for (String s : buildLog.keySet()) {
            Util.parseLocation(s).getBlock().setType(Material.AIR);
        }
        buildLog.clear();
        player.getPlayer().teleport(playerSpawn);
        score = 0;
        stopwatch.stop();
        generateFirst(playerSpawn, blockSpawn);
    }

    /**
     * Generates the next parkour block, choosing between structures and normal jumps.
     * If it's a normal jump, it will get a random distance between them and whether it
     * goes up or not.
     */
    public void generateNext() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (defaultChances.size() == 0) {
            // Counting with % above 100%
            int index = 0;
            for (int i = 0; i < Configurable.NORMAL; i++) {
                defaultChances.put(index, 0);
                index++;
            }
//            for (int i = 0; i < GeneratorChance.STRUCTURES; i++) {
//                defaultChances.put(index, 1);
//                index++;
//            } // todo remove comment
        }

        switch (defaultChances.get(random.nextInt(defaultChances.size()))) {
            case 0:

                int one = Configurable.MAXED_ONE_BLOCK;
                int two = Configurable.MAXED_TWO_BLOCK;
                int three = Configurable.MAXED_THREE_BLOCK;
                int four = Configurable.MAXED_FOUR_BLOCK;
                if (score <= Configurable.MULTIPLIER) {
                    one = (int) (Configurable.NORMAL_ONE_BLOCK + (multiplierDecreases.get(1) * score));
                    two = (int) (Configurable.NORMAL_TWO_BLOCK + (multiplierDecreases.get(2) * score));
                    three = (int) (Configurable.NORMAL_THREE_BLOCK + (multiplierDecreases.get(3) * score));
                    four = (int) (Configurable.NORMAL_FOUR_BLOCK + (multiplierDecreases.get(4) * score));
                }
                if (distanceChances.size() == 0) {
                    int index = 0;
                    for (int i = 0; i < one; i++) {
                        distanceChances.put(index, 1);
                        index++;
                    }
                    for (int i = 0; i < two; i++) {
                        distanceChances.put(index, 2);
                        index++;
                    }
                    for (int i = 0; i < three; i++) {
                        distanceChances.put(index, 3);
                        index++;
                    }
                    for (int i = 0; i < four; i++) {
                        distanceChances.put(index, 4);
                        index++;
                    }
                }

                if (heightChances.size() == 0) {
                    int index = 0;
                    for (int i = 0; i < Configurable.NORMAL_UP; i++) {
                        heightChances.put(index, -1);
                        index++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_LEVEL; i++) {
                        heightChances.put(index, 0);
                        index++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_DOWN; i++) {
                        heightChances.put(index, 1);
                        index++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_DOWN2; i++) {
                        heightChances.put(index, 2);
                        index++;
                    }
                }

                int height = heightChances.get(random.nextInt(heightChances.size())) * -1; // -1 * -1 = +1 when y should be +1, so this works
                int gap = distanceChances.get(random.nextInt(distanceChances.size())) + 1;
                List<Block> possible = getPossible(gap - height, height);
                if (possible.size() == 0) {
                    return;
                }
                Block chosen = possible.get(random.nextInt(possible.size()));
                chosen.setType(player.randomMaterial());
                lastSpawn = chosen.getLocation().clone();

                break;
            case 1:
                System.out.println("other");
                break;
        }

        int listSize = player.blockLead + 5; // the size of the queue of parkour blocks
        listSize--;
        List<String> locations = new ArrayList<>(buildLog.keySet());
        if (locations.size() > listSize) {
            locations = locations.subList(0, listSize);
        }
        buildLog.clear();
        buildLog.put(Util.toString(lastSpawn, false), 0);
        for (int i = 0; i < locations.size(); i++) {
            String location = locations.get(i);
            if (location != null) {
                buildLog.put(location, i + 1);
            }
        }
    }

    /**
     * Generates the first few blocks (which come off the spawn island)
     *
     * @param   spawn
     *          The spawn of the player
     * @param   block
     *          The location used to begin the parkour of off
     */
    public void generateFirst(Location spawn, Location block) {
        playerSpawn = spawn.clone();
        blockSpawn = block.clone();
        lastSpawn = block.clone();
        generateNext(player.blockLead);
    }

    // Generates in a loop
    private void generateNext(int amount) {
        for (int i = 0; i < amount; i++) {
            generateNext();
        }
    }

    // Gets all possible parkour locations
    private List<Block> getPossible(double radius, int dy) {
        List<Block> possible = new ArrayList<>();
        World world = lastSpawn.getWorld();
        Location base = lastSpawn.add(0, dy, 0);
        double detail = (radius * 12);
        double increment = (2 * Math.PI) / detail;

        double heightGap = dy >= 0 ? Configurable.HEIGHT_GAP - dy : Configurable.HEIGHT_GAP - (dy + 1); // if dy <= 2 set max gap between blocks to default -1,
                                                                                                        // otherwise jump will be impossible
        for (int i = 0; i < detail; i++) {
            double angle = i * increment;
            double x = base.getX() + (radius * Math.cos(angle));
            double z = base.getZ() + (radius * Math.sin(angle));
            Block block = new Location(world, x, base.getBlockY(), z).getBlock();
            if (base.clone().subtract(block.getLocation()).toVector().getZ() > 0 // direction change
                    && block.getLocation().distance(base) <= heightGap) {
                possible.add(block);
            }
        }
        return possible;
    }

    public static class Configurable {

        public static int NORMAL;
        public static int STRUCTURES;

        public static int NORMAL_ONE_BLOCK;
        public static int NORMAL_TWO_BLOCK;
        public static int NORMAL_THREE_BLOCK;
        public static int NORMAL_FOUR_BLOCK;

        public static int NORMAL_UP;
        public static int NORMAL_LEVEL;
        public static int NORMAL_DOWN;
        public static int NORMAL_DOWN2;

        // Advanced settings
        public static int GENERATOR_CHECK;
        public static double HEIGHT_GAP;
        public static double MULTIPLIER;

        public static int MAXED_ONE_BLOCK;
        public static int MAXED_TWO_BLOCK;
        public static int MAXED_THREE_BLOCK;
        public static int MAXED_FOUR_BLOCK;

        public static void init() {
            FileConfiguration file = WITP.getConfiguration().getFile("generation");
            NORMAL = file.getInt("generation.normal-jump.chance");
            STRUCTURES = file.getInt("generation.structures.chance");

            NORMAL_ONE_BLOCK = file.getInt("generation.normal-jump.1-block");
            NORMAL_TWO_BLOCK = file.getInt("generation.normal-jump.2-block");
            NORMAL_THREE_BLOCK = file.getInt("generation.normal-jump.3-block");
            NORMAL_FOUR_BLOCK = file.getInt("generation.normal-jump.4-block");

            NORMAL_UP = file.getInt("generation.normal-jump.up");
            NORMAL_LEVEL = file.getInt("generation.normal-jump.level");
            NORMAL_DOWN = file.getInt("generation.normal-jump.down");
            NORMAL_DOWN2 = file.getInt("generation.normal-jump.down2");

            // Advanced settings
            GENERATOR_CHECK = file.getInt("advanced.generator-check");
            HEIGHT_GAP = file.getDouble("advanced.height-gap");
            MULTIPLIER = file.getInt("advanced.maxed-multiplier");

            MAXED_ONE_BLOCK = file.getInt("advanced.maxed-values.1-block");
            MAXED_TWO_BLOCK = file.getInt("advanced.maxed-values.2-block");
            MAXED_THREE_BLOCK = file.getInt("advanced.maxed-values.3-block");
            MAXED_FOUR_BLOCK = file.getInt("advanced.maxed-values.4-block");
        }
    }
}