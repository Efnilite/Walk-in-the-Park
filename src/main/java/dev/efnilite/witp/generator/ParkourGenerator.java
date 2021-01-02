package dev.efnilite.witp.generator;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.BlockGenerateEvent;
import dev.efnilite.witp.events.PlayerFallEvent;
import dev.efnilite.witp.events.PlayerScoreEvent;
import dev.efnilite.witp.generator.subarea.SubareaPoint;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.particle.ParticleData;
import dev.efnilite.witp.util.particle.Particles;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link dev.efnilite.witp.ParkourPlayer} has.<br>
 * <p>
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
    public double borderOffset;

    /**
     * The time of the player's current session
     *
     * @see Stopwatch#toString()
     */
    public String time = "0ms";
    public SubareaPoint.Data data;
    /**
     * The heading of the parkour
     */
    public Vector heading;

    private int totalScore;
    private int structureCooldown;
    private boolean deleteStructure;
    private boolean stopped;
    private Location lastSpawn;
    private Location lastPlayer;
    private Location previousSpawn;

    private Location playerSpawn;
    private Location blockSpawn;
    private List<Block> structureBlocks;

    private final Stopwatch stopwatch;
    private final ParkourPlayer player;
    private final LinkedHashMap<String, Integer> buildLog;
    private final HashMap<Integer, Integer> distanceChances;
    private final HashMap<Integer, Integer> specialChances;
    private final HashMap<Integer, Integer> heightChances;
    private final HashMap<Integer, Integer> defaultChances;
    private final HashMap<Integer, Double> multiplierDecreases;

    private static final ParticleData<?> particleData = new ParticleData<>(Particle.SPELL_INSTANT, null, 20, 0.4,
            0.5, 1, 0.5);

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param player The player associated with this generator
     */
    public ParkourGenerator(ParkourPlayer player) {
        this.score = 0;
        this.totalScore = 0;
        this.borderOffset = Configurable.BORDER_SIZE / 2.0;
        this.stopped = false;
        this.player = player;
        this.structureCooldown = 30;
        this.lastSpawn = player.getPlayer().getLocation().clone();
        this.lastPlayer = lastSpawn.clone();
        this.distanceChances = new HashMap<>();
        this.heightChances = new HashMap<>();
        this.specialChances = new HashMap<>();
        this.buildLog = new LinkedHashMap<>();
        this.defaultChances = new HashMap<>();
        this.stopwatch = new Stopwatch();
        this.structureBlocks = new ArrayList<>();
        this.multiplierDecreases = new HashMap<>();
        this.deleteStructure = false;

        double multiplier = Configurable.MULTIPLIER;
        multiplierDecreases.put(1, (Configurable.MAXED_ONE_BLOCK - Configurable.NORMAL_ONE_BLOCK) / multiplier);
        multiplierDecreases.put(2, (Configurable.MAXED_TWO_BLOCK - Configurable.NORMAL_TWO_BLOCK) / multiplier);
        multiplierDecreases.put(3, (Configurable.MAXED_THREE_BLOCK - Configurable.NORMAL_THREE_BLOCK) / multiplier);
        multiplierDecreases.put(4, (Configurable.MAXED_FOUR_BLOCK - Configurable.NORMAL_FOUR_BLOCK) / multiplier);

        Tasks.syncRepeat(new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    this.cancel();
                    return;
                }
                Location playerLoc = player.getPlayer().getLocation();
                if (lastPlayer.getY() - playerLoc.getY() > 10 && playerSpawn.distance(playerLoc) > 5) {
                    new PlayerFallEvent(player).call();
                    reset(true);
                    return;
                }
                Block at = playerLoc.getBlock();
                Block current = playerLoc.clone().subtract(0, 1, 0).getBlock();
                if (at.getType() != Material.AIR) {
                    current = at;
                }
                if (current.getType() != Material.AIR) {
                    previousSpawn = lastPlayer.clone();
                    lastPlayer = current.getLocation();
                    if (structureBlocks.contains(current) && current.getType() == Material.RED_WOOL && !deleteStructure) {
                        score += 10;
                        if (player.useSpecial) {
                            player.updateScoreboard();
                        }
                        structureCooldown = 30;
                        generateNext(player.blockLead);
                        deleteStructure = true;
                        return;
                    }
                    String last = Util.toString(lastPlayer, false);
                    Integer latest = buildLog.get(last);
                    if (latest != null) {
                        if (!(Util.toString(previousSpawn, true).equals(Util.toString(lastPlayer, true)))) {
                            if (!stopwatch.hasStarted()) {
                                stopwatch.start();
                            }
                            score++;
                            totalScore++;
                            if (Configurable.REWARDS && totalScore % Configurable.REWARDS_INTERVAL == 0 && Configurable.REWARDS_COMMAND != null) {
                                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), Configurable.REWARDS_COMMAND);
                                player.send(Configurable.REWARDS_MESSAGE);
                            }
                            new PlayerScoreEvent(player).call();
                            if (player.useSpecial) {
                                player.updateScoreboard();
                            }
                            List<String> locations = new ArrayList<>(buildLog.keySet());
                            int lastIndex = locations.indexOf(last) + 1;
                            int size = locations.size();
                            for (int i = lastIndex; i < size; i++) {
                                Util.parseLocation(locations.get(i)).getBlock().setType(Material.AIR);
                            }
                            if (deleteStructure) {
                                deleteStructure = false;
                                for (Block block : structureBlocks) {
                                    block.setType(Material.AIR);
                                }
                                structureBlocks.clear();
                            }
                        }

                        int difference = player.blockLead - latest;
                        if (difference > 0) {
                            generateNext(Math.abs(difference));
                        }
                    }
                }
                time = stopwatch.toString();
                player.getPlayer().setSaturation(20);
                if (player.useSpecial) {
                    player.updateScoreboard();
                }
            }
        }, Configurable.GENERATOR_CHECK);
    }

    /**
     * Resets the parkour
     *
     * @param   regenerate
     *          false if this is the last reset (when the player leaves), true for resets by falling
     */
    public void reset(boolean regenerate) {
        if (!regenerate) {
            stopped = true;
        }
        for (Block block : structureBlocks) {
            block.setType(Material.AIR);
        }
        structureBlocks.clear();
        for (String s : buildLog.keySet()) {
            Util.parseLocation(s).getBlock().setType(Material.AIR);
        }
        structureCooldown = 30;
        buildLog.clear();
        player.getPlayer().teleport(playerSpawn);
        if (player.showDeathMsg) {
            String message;
            if (score == player.highScore) {
                message = "You tied your high score!";
            } else if (score > player.highScore) {
                message = "You beat your high score by " + (score - player.highScore) + " points!";
            } else {
                message = "You missed your high score by " + (player.highScore - score) + " points!";
            }
            if (score > player.highScore) {
                player.setHighScore(score);
            }
            player.send("&7----------------------------------------", "&aYour score: &f" +
                            score, "&aYour time: &f" + time, "&aYour highscore: &f" + player.highScore, "&7" + message,
                    "&7----------------------------------------");
        } else {
            if (score > player.highScore) {
                player.setHighScore(score);
            }
        }
        score = 0;
        stopwatch.stop();
        if (regenerate) {
            generateFirst(playerSpawn, blockSpawn);
        }
    }

    /**
     * Generates the next parkour block, choosing between structures and normal jumps.
     * If it's a normal jump, it will get a random distance between them and whether it
     * goes up or not.
     * <p>
     * Note: please be cautious when messing about with parkour generation, since even simple changes
     * could break the entire plugin
     */
    public void generateNext() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (defaultChances.size() == 0) {
            int index = 0;
            for (int i = 0; i < Configurable.NORMAL; i++) {
                defaultChances.put(index, 0);
                index++;
            }
            for (int i = 0; i < Configurable.STRUCTURES; i++) {
                defaultChances.put(index, 1);
                index++;
            }
            for (int i = 0; i < Configurable.SPECIAL; i++) {
                defaultChances.put(index, 2);
                index++;
            }
        }

        int def = defaultChances.get(random.nextInt(defaultChances.size())); // 0 = normal, 1 = structures
        int special = def == 2 ? 1 : 0; // 1 = yes, 0 = no
        if (special == 1) {
            def = 0;
        } else {
            def = structureCooldown == 0 && player.useStructures ? def : 0;
        }
        switch (def) {
            case 0:
                if (isNearBorder(lastSpawn.clone().toVector()) && score > 0) {
                    int copy = score;
                    reset(true);
                    score = copy;
                    player.send("&cSorry for the inconvenience, but you have been teleported back to spawn");
                    player.send("&cYou can continue adding to your score");
                    return;
                }

                if (player.useDifficulty || distanceChances.size() == 0) {
                    int one = Configurable.MAXED_ONE_BLOCK;
                    int two = Configurable.MAXED_TWO_BLOCK;
                    int three = Configurable.MAXED_THREE_BLOCK;
                    int four = Configurable.MAXED_FOUR_BLOCK;
                    if (player.useDifficulty) {
                        if (score <= Configurable.MULTIPLIER) {
                            one = (int) (Configurable.NORMAL_ONE_BLOCK + (multiplierDecreases.get(1) * score));
                            two = (int) (Configurable.NORMAL_TWO_BLOCK + (multiplierDecreases.get(2) * score));
                            three = (int) (Configurable.NORMAL_THREE_BLOCK + (multiplierDecreases.get(3) * score));
                            four = (int) (Configurable.NORMAL_FOUR_BLOCK + (multiplierDecreases.get(4) * score));
                        }
                    } else {
                        one = Configurable.NORMAL_ONE_BLOCK;
                        two = Configurable.NORMAL_TWO_BLOCK;
                        three = Configurable.NORMAL_THREE_BLOCK;
                        four = Configurable.NORMAL_FOUR_BLOCK;
                    }
                    distanceChances.clear();
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
                    int index1 = 0;
                    for (int i = 0; i < Configurable.NORMAL_UP; i++) {
                        heightChances.put(index1, 1);
                        index1++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_LEVEL; i++) {
                        heightChances.put(index1, 0);
                        index1++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_DOWN; i++) {
                        heightChances.put(index1, -1);
                        index1++;
                    }
                    for (int i = 0; i < Configurable.NORMAL_DOWN2; i++) {
                        heightChances.put(index1, -2);
                        index1++;
                    }
                }

                int height = 0;
                int deltaYMin = lastSpawn.getBlockY() - Configurable.MIN_Y;
                int deltaYMax = lastSpawn.getBlockY() - Configurable.MAX_Y;
                if (deltaYMin < 20) { // buffer of 20, so the closer to the max/min the more chance of opposite
                    int delta = (deltaYMin - 20) * -1;
                    int chanceRise = delta * 5;
                    if (chanceRise >= random.nextInt(100) + 1) {
                        height = 1;
                    } else {
                        height = heightChances.get(random.nextInt(heightChances.size()));
                    }
                } else if (deltaYMax > -20) {
                    int delta = deltaYMax + 20;
                    int chanceRise = delta * 5;
                    if (chanceRise >= random.nextInt(100) + 1) {
                        switch (random.nextInt(2)) {
                            case 0:
                                height = -2;
                                break;
                            case 1:
                                height = -1;
                                break;
                        }
                    } else {
                        height = heightChances.get(random.nextInt(heightChances.size()));
                    }
                } else {
                    height = heightChances.get(random.nextInt(heightChances.size()));
                }
                double gap = distanceChances.get(random.nextInt(distanceChances.size())) + 1;

                BlockData material = player.randomMaterial().createBlockData();
                if (special == 1 && player.useSpecial) {
                    if (specialChances.size() == 0) {
                        int index = 0;
                        for (int i = 0; i < Configurable.SPECIAL_ICE; i++) {
                            specialChances.put(index, 0);
                            index++;
                        }
                        for (int i = 0; i < Configurable.SPECIAL_SLAB; i++) {
                            specialChances.put(index, 1);
                            index++;
                        }
                        for (int i = 0; i < Configurable.SPECIAL_PANE; i++) {
                            specialChances.put(index, 2);
                            index++;
                        }
                    }

                    int spec = specialChances.get(random.nextInt(specialChances.size() - 1));
                    switch (spec) {
                        case 0: // ice
                            material = Material.PACKED_ICE.createBlockData();
                            gap++;
                            break;
                        case 1: // slab
                            material = Material.SMOOTH_QUARTZ_SLAB.createBlockData();
                            ((Slab) material).setType(Slab.Type.BOTTOM);
                            height = Math.min(height, 0);
                            break;
                        case 2: // pane
                            material = Material.GLASS_PANE.createBlockData();
                            gap -= 0.5;
                            break;
                    }
                }

                Location local = lastSpawn.clone();
                List<Block> possible = getPossible(gap - height, height);
                if (possible.size() == 0) {
                    lastSpawn = local;
                    generateNext();
                    return;
                }

                Block chosen = possible.get(random.nextInt(possible.size()));
                chosen.setBlockData(material);
                new BlockGenerateEvent(chosen, this, player).call();
                lastSpawn = chosen.getLocation().clone();

                if (player.useParticles) {
                    particleData.setType(Configurable.PARTICLE_TYPE);
                    Particles.draw(lastSpawn.clone().add(0, 1, 0), particleData);
                    player.getPlayer().playSound(lastSpawn.clone(), Configurable.SOUND_TYPE, 4, Configurable.SOUND_PITCH);
                }

                if (structureCooldown > 0) {
                    structureCooldown--;
                }
                break;
            case 1:
                File folder = new File(WITP.getInstance().getDataFolder() + "/structures/");
                List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.contains("parkour-")));
                File structure = files.get(random.nextInt(files.size() - 1));

                structureCooldown = 30;
                int gapStructure = distanceChances.get(random.nextInt(distanceChances.size())) + 1;
                List<Block> possibleStructure = getPossible(gapStructure, 0);
                if (possibleStructure.size() == 0) {
                    return;
                }
                Block chosenStructure = possibleStructure.get(random.nextInt(possibleStructure.size()));

                StructureData data = WITP.getVersionManager().placeAt(structure, chosenStructure.getLocation());
                structureBlocks = new ArrayList<>(data.blocks);
                lastSpawn = data.end.clone();
                break;
        }

        int listSize = player.blockLead + 7; // the size of the queue of parkour blocks
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
     *
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
        int y = base.getBlockY();
        double detail = (radius * 8);
        double increment = (2 * Math.PI) / detail;

        double heightGap = dy >= 0 ? Configurable.HEIGHT_GAP - dy : Configurable.HEIGHT_GAP - (dy + 1);
        // if dy <= 2 set max gap between blocks to default -1,
        // otherwise jump will be impossible
        for (int i = 0; i < detail; i++) {
            double angle = i * increment;
            double x = base.getX() + (radius * Math.cos(angle));
            double z = base.getZ() + (radius * Math.sin(angle));
            Block block = new Location(world, x, y, z).getBlock();
            if (isFollowing(base.clone().subtract(block.getLocation()).toVector()) // direction change
                    && block.getLocation().distance(base) <= heightGap) {
                possible.add(block);
            }
        }
        return possible;
    }

    /**
     * Checks if a vector is following the assigned heading
     *
     * @param vector The direction vector between the latest spawned parkour block and a new possible block
     * @return true if the vector is following the heading assigned to param heading
     */
    public boolean isFollowing(Vector vector) {
        if (heading.getBlockZ() != 0) {
            return vector.getZ() * heading.getZ() > 0;
        } else if (heading.getBlockX() != 0) {
            return vector.getX() * heading.getX() < 0;
        } else {
            Verbose.error("Invalid heading vector: " + heading.toString());
            return false;
        }
    }

    /**
     * If the vector is near the border
     *
     * @param vector The vector
     */
    public boolean isNearBorder(Vector vector) {
        return Math.abs(borderOffset - Math.abs(vector.getX())) < 25 || Math.abs(borderOffset - Math.abs(vector.getZ())) < 25;
    }

    public boolean isNearIsland(Vector vector) {
        return vector.distance(playerSpawn.toVector()) < 50;
    }

    /**
     * Class for variables required in generating without accessing the file a lot (constants)
     */
    public static class Configurable {

        public static int NORMAL;
        public static int SPECIAL;
        public static int STRUCTURES;

        public static int SPECIAL_ICE;
        public static int SPECIAL_SLAB;
        public static int SPECIAL_PANE;

        public static int NORMAL_ONE_BLOCK;
        public static int NORMAL_TWO_BLOCK;
        public static int NORMAL_THREE_BLOCK;
        public static int NORMAL_FOUR_BLOCK;

        public static int NORMAL_UP;
        public static int NORMAL_LEVEL;
        public static int NORMAL_DOWN;
        public static int NORMAL_DOWN2;

        public static int MAX_Y;
        public static int MIN_Y;

        // Config stuff
        public static boolean REWARDS;
        public static int REWARDS_INTERVAL;
        public static String REWARDS_COMMAND;
        public static String REWARDS_MESSAGE;

        public static Sound SOUND_TYPE;
        public static int SOUND_PITCH;
        public static Particle PARTICLE_TYPE;

        // Advanced settings
        public static double BORDER_SIZE;
        public static int GENERATOR_CHECK;
        public static double HEIGHT_GAP;
        public static double MULTIPLIER;

        public static int MAXED_ONE_BLOCK;
        public static int MAXED_TWO_BLOCK;
        public static int MAXED_THREE_BLOCK;
        public static int MAXED_FOUR_BLOCK;

        public static void init() {
            FileConfiguration file = WITP.getConfiguration().getFile("generation");
            FileConfiguration config = WITP.getConfiguration().getFile("config");
            NORMAL = file.getInt("generation.normal-jump.chance");
            STRUCTURES = file.getInt("generation.structures.chance");
            SPECIAL = file.getInt("generation.normal-jump.special.chance");

            SPECIAL_ICE = file.getInt("generation.normal-jump.special.ice");
            SPECIAL_SLAB = file.getInt("generation.normal-jump.special.slab");
            SPECIAL_PANE = file.getInt("generation.normal-jump.special.pane");

            NORMAL_ONE_BLOCK = file.getInt("generation.normal-jump.1-block");
            NORMAL_TWO_BLOCK = file.getInt("generation.normal-jump.2-block");
            NORMAL_THREE_BLOCK = file.getInt("generation.normal-jump.3-block");
            NORMAL_FOUR_BLOCK = file.getInt("generation.normal-jump.4-block");

            NORMAL_UP = file.getInt("generation.normal-jump.up");
            NORMAL_LEVEL = file.getInt("generation.normal-jump.level");
            NORMAL_DOWN = file.getInt("generation.normal-jump.down");
            NORMAL_DOWN2 = file.getInt("generation.normal-jump.down2");

            MAX_Y = file.getInt("generation.settings.max-y");
            MIN_Y = file.getInt("generation.settings.min-y");

            // Config stuff
            REWARDS = config.getBoolean("rewards.enabled");
            REWARDS_INTERVAL = config.getInt("rewards.interval");
            REWARDS_COMMAND = config.getString("rewards.command").replaceAll("/", "");
            if (REWARDS_COMMAND.equalsIgnoreCase("null")) {
                REWARDS_COMMAND = null;
            }
            REWARDS_MESSAGE = config.getString("rewards.message");
            if (REWARDS_MESSAGE.equalsIgnoreCase("null")) {
                REWARDS_MESSAGE = null;
            }

            SOUND_TYPE = Sound.valueOf(config.getString("particles.sound-type").toUpperCase());
            SOUND_PITCH = config.getInt("particles.sound-pitch");
            PARTICLE_TYPE = Particle.valueOf(config.getString("particles.particle-type").toUpperCase());

            // Advanced settings
            BORDER_SIZE = file.getDouble("advanced.border-size");
            GENERATOR_CHECK = file.getInt("advanced.generator-check");
            HEIGHT_GAP = file.getDouble("advanced.height-gap");
            MULTIPLIER = file.getInt("advanced.maxed-multiplier");

            MAXED_ONE_BLOCK = file.getInt("advanced.maxed-values.1-block");
            MAXED_TWO_BLOCK = file.getInt("advanced.maxed-values.2-block");
            MAXED_THREE_BLOCK = file.getInt("advanced.maxed-values.3-block");
            MAXED_FOUR_BLOCK = file.getInt("advanced.maxed-values.4-block");
        }
    }

    public static class StructureData {

        public Location end;
        public List<Block> blocks;

        public StructureData(Location location, List<Block> blocks) {
            this.end = location;
            this.blocks = blocks;
        }
    }
}