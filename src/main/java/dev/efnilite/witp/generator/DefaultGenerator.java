package dev.efnilite.witp.generator;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.BlockGenerateEvent;
import dev.efnilite.witp.events.PlayerFallEvent;
import dev.efnilite.witp.events.PlayerScoreEvent;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.schematic.RotationAngle;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.SchematicAdjuster;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.particle.ParticleData;
import dev.efnilite.witp.util.particle.Particles;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link ParkourPlayer} has.<br>
 * <p>
 * Important notice: tempering with details in this class could result in complete malfunction of code since
 * this class has been meticulously made using a lot of cross-references. Same goes for
 * {@link dev.efnilite.witp.generator.subarea.SubareaDivider}.
 *
 * @author Efnilite
 */
public class DefaultGenerator extends ParkourGenerator {

    private BukkitRunnable task;

    protected int totalScore;
    protected int structureCooldown;
    protected boolean deleteStructure;
    protected boolean stopped;
    protected boolean waitForSchematicCompletion;

    protected Location lastSpawn;
    protected Location lastPlayer;
    protected Location previousSpawn;
    protected Location latestLocation; // to disallow 1 block infinite point glitch

    protected Location playerSpawn;
    protected Location blockSpawn;
    protected List<Block> structureBlocks;

    protected final Queue<Block> generatedHistory;
    protected final LinkedHashMap<String, Integer> buildLog;
    protected final HashMap<Integer, Integer> distanceChances;
    protected final HashMap<Integer, Integer> specialChances;
    protected final HashMap<Integer, Integer> heightChances;
    protected final HashMap<Integer, Integer> defaultChances;
    protected final HashMap<Integer, Double> multiplierDecreases;

    protected static final ParticleData<?> PARTICLE_DATA = new ParticleData<>(Particle.SPELL_INSTANT, null, 10, 0,
            0, 0, 0);

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param player The player associated with this generator
     */
    public DefaultGenerator(@NotNull ParkourPlayer player) {
        super(player);
        Verbose.verbose("Init of DefaultGenerator of " + player.getPlayer().getName());
        this.score = 0;
        this.totalScore = 0;
        this.stopped = false;
        this.waitForSchematicCompletion = false;
        this.structureCooldown = 20;
        this.lastSpawn = player.getLocation().clone();
        this.lastPlayer = lastSpawn.clone();
        this.latestLocation = lastSpawn.clone();
        this.generatedHistory = new LinkedList<>();
        this.distanceChances = new HashMap<>();
        this.heightChances = new HashMap<>();
        this.specialChances = new HashMap<>();
        this.buildLog = new LinkedHashMap<>();
        this.defaultChances = new HashMap<>();
        this.structureBlocks = new ArrayList<>();
        this.multiplierDecreases = new HashMap<>();
        this.deleteStructure = false;

        double multiplier = Option.MULTIPLIER;
        multiplierDecreases.put(1, (Option.MAXED_ONE_BLOCK - Option.NORMAL_ONE_BLOCK) / multiplier);
        multiplierDecreases.put(2, (Option.MAXED_TWO_BLOCK - Option.NORMAL_TWO_BLOCK) / multiplier);
        multiplierDecreases.put(3, (Option.MAXED_THREE_BLOCK - Option.NORMAL_THREE_BLOCK) / multiplier);
        multiplierDecreases.put(4, (Option.MAXED_FOUR_BLOCK - Option.NORMAL_FOUR_BLOCK) / multiplier);
    }

    /**
     * Starts the check
     */
    @Override
    public void start() {
        Verbose.verbose("Starting generator of " + player.getPlayer().getName());
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    this.cancel();
                    return;
                }
                Location playerLoc = player.getLocation();

                // Fall check
                if (lastPlayer.getY() - playerLoc.getY() > 10 && playerSpawn.distance(playerLoc) > 5) {
                    new PlayerFallEvent(player).call();
                    reset(true);
                    return;
                }

                // If the block below
                Block at = playerLoc.getBlock();
                Block current = playerLoc.clone().subtract(0, 1, 0).getBlock();
                if (at.getType() != Material.AIR) {
                    current = at;
                }

                updateTime();
                player.getPlayer().setSaturation(20);
                updateSpectators();

                if (current.getLocation().equals(latestLocation)) {
                    player.updateScoreboard();
                    return;
                }

                tick();
                if (current.getType() != Material.AIR) {
                    previousSpawn = lastPlayer.clone();
                    lastPlayer = current.getLocation();
                    // Structure deletion check
                    if (structureBlocks.contains(current) && current.getType() == Material.RED_WOOL && !deleteStructure) {
                        for (int i = 0; i < 10; i++) {
                            score++;
                            checkRewards();
                        }
                        waitForSchematicCompletion = false;
                        structureCooldown = 20;
                        generate(player.blockLead);
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

                            latestLocation = current.getLocation();

                            if (!Option.ALL_POINTS) {
                                addPoint();
                            } else if (score == 0) {
                                addPoint();
                            }

                            List<String> locations = new ArrayList<>(buildLog.keySet()); // delete blocks
                            int lastIndex = locations.indexOf(last) + 1;
                            int size = locations.size();
                            for (int i = lastIndex; i < size; i++) {
                                Block block = Util.parseLocation(locations.get(i)).getBlock();
                                if (block.getType() != Material.AIR) {
                                    if (Option.ALL_POINTS) {
                                        addPoint();
                                    }
                                    block.setType(Material.AIR);
                                }
                            }

                            new PlayerScoreEvent(player).call();
                            if (deleteStructure) {
                                deleteStructure();
                            }
                        }

                        int difference = player.blockLead - latest;
                        if (difference > 0) {
                            generate(Math.abs(difference));
                        }
                    }
                }
                player.updateScoreboard();
            }
        };
        Tasks.defaultSyncRepeat(task, Option.GENERATOR_CHECK);
    }

    private void addPoint() {
        score++;
        totalScore++;
        score();
        checkRewards();
    }

    private void checkRewards() {
        // Rewards
        HashMap<Integer, List<String>> scores = Option.REWARDS_SCORES;
        if ((Option.REWARDS_INTERVAL > 0 && totalScore % Option.REWARDS_INTERVAL == 0)
                || (scores.size() > 0 && scores.containsKey(score))) {
            if (Option.REWARDS) {
                if (scores.containsKey(score) && scores.get(score) != null) {
                    List<String> commands = scores.get(score);
                    if (commands != null) {
                        for (String command : commands) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
                                    command.replaceAll("%player%", player.getPlayer().getName()));
                        }
                    }
                }
                if (Option.INTERVAL_REWARDS_SCORES != null) {
                    for (String command : Option.INTERVAL_REWARDS_SCORES) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
                                command.replaceAll("%player%", player.getPlayer().getName()));
                    }
                }
                if (Option.REWARDS_MONEY != 0) {
                    Util.depositPlayer(player.getPlayer(), Option.REWARDS_MONEY);
                }
                if (Option.REWARDS_MESSAGE != null) {
                    player.send(Option.REWARDS_MESSAGE);
                }
            }
        }
    }

    public void score() { }

    public void tick() { }

    /**
     * Resets the parkour
     *
     * @param   regenerate
     *          false if this is the last reset (when the player leaves), true for resets by falling
     */
    @Override
    public void reset(boolean regenerate) {
        if (!regenerate) {
            stopped = true;
            task.cancel();
        }
        for (Block block : generatedHistory) {
            block.setType(Material.AIR);
        }
        generatedHistory.clear();

        waitForSchematicCompletion = false;
        player.saveGame();
        deleteStructure();
        buildLog.clear();
        player.getPlayer().teleport(playerSpawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
        int score = this.score;
        String time = this.time;
        String diff = player.calculateDifficultyScore();
        if (player.showDeathMsg && regenerate && time != null) {
            String message;
            int number = 0;
            if (score == player.highScore) {
                message = "message.tied";
            } else if (score > player.highScore) {
                number = score - player.highScore;
                message = "message.beat";
            } else {
                number = player.highScore - score;
                message = "message.miss";
            }
            if (score > player.highScore) {
                player.setHighScore(score, time, diff);
            }
            player.sendTranslated("divider");
            player.sendTranslated("score", Integer.toString(score));
            player.sendTranslated("time", time);
            player.sendTranslated("highscore", Integer.toString(player.highScore));
            player.sendTranslated(message, Integer.toString(number));
            player.sendTranslated("divider");
        } else {
            if (score >= player.highScore) {
                player.setHighScore(score, time, diff);
            }
        }
        this.score = 0;
        stopwatch.stop();
        if (regenerate) {
            generateFirst(playerSpawn, blockSpawn);
        }
    }

    protected void deleteStructure() {
        for (Block block : structureBlocks) {
            block.setType(Material.AIR);
        }

        structureBlocks.clear();
        deleteStructure = false;
        structureCooldown = 20;
    }

    /**
     * Generates the next parkour block, choosing between structures and normal jumps.
     * If it's a normal jump, it will get a random distance between them and whether it
     * goes up or not.
     * <p>
     * Note: please be cautious when messing about with parkour generation, since even simple changes
     * could break the entire plugin
     */
    @Override
    public void generate() {
        if (waitForSchematicCompletion) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (defaultChances.size() == 0) {
            int index = 0;
            for (int i = 0; i < Option.NORMAL; i++) {
                defaultChances.put(index, 0);
                index++;
            }
            for (int i = 0; i < Option.STRUCTURES; i++) {
                defaultChances.put(index, 1);
                index++;
            }
            for (int i = 0; i < Option.SPECIAL; i++) {
                defaultChances.put(index, 2);
                index++;
            }
        }

        int def = defaultChances.get(random.nextInt(defaultChances.size())); // 0 = normal, 1 = structures, 2 = special
        int special = def == 2 ? 1 : 0; // 1 = yes, 0 = no
        if (special == 1) {
            def = 0;
        } else {
            def = structureCooldown == 0 && player.useStructure ? def : 0;
        }
        switch (def) {
            case 0:
                if (isNearBorder(lastSpawn.clone().toVector()) && score > 0) {
                    heading.rotateUnitRight(); // reverse heading
                }

                if (player.useDifficulty || distanceChances.size() == 0) {
                    int one = Option.MAXED_ONE_BLOCK;
                    int two = Option.MAXED_TWO_BLOCK;
                    int three = Option.MAXED_THREE_BLOCK;
                    int four = Option.MAXED_FOUR_BLOCK;
                    if (player.useDifficulty) {
                        if (score <= Option.MULTIPLIER) {
                            one = (int) (Option.NORMAL_ONE_BLOCK + (multiplierDecreases.get(1) * score));
                            two = (int) (Option.NORMAL_TWO_BLOCK + (multiplierDecreases.get(2) * score));
                            three = (int) (Option.NORMAL_THREE_BLOCK + (multiplierDecreases.get(3) * score));
                            four = (int) (Option.NORMAL_FOUR_BLOCK + (multiplierDecreases.get(4) * score));
                        }
                    } else {
                        one = Option.NORMAL_ONE_BLOCK;
                        two = Option.NORMAL_TWO_BLOCK;
                        three = Option.NORMAL_THREE_BLOCK;
                        four = Option.NORMAL_FOUR_BLOCK;
                    }
                    distanceChances.clear();
                    int index = 0;
                    for (int i = 0; i < one; i++) { // regenerate the chances for distance
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

                if (heightChances.size() == 0) { // regenerate the chances for height
                    int index1 = 0;
                    for (int i = 0; i < Option.NORMAL_UP; i++) {
                        heightChances.put(index1, 1);
                        index1++;
                    }
                    for (int i = 0; i < Option.NORMAL_LEVEL; i++) {
                        heightChances.put(index1, 0);
                        index1++;
                    }
                    for (int i = 0; i < Option.NORMAL_DOWN; i++) {
                        heightChances.put(index1, -1);
                        index1++;
                    }
                    for (int i = 0; i < Option.NORMAL_DOWN2; i++) {
                        heightChances.put(index1, -2);
                        index1++;
                    }
                }

                int height = 0;
                int deltaYMin = lastSpawn.getBlockY() - Option.MIN_Y;
                int deltaYMax = lastSpawn.getBlockY() - Option.MAX_Y;
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
                        for (int i = 0; i < Option.SPECIAL_ICE; i++) {
                            specialChances.put(index, 0);
                            index++;
                        }
                        for (int i = 0; i < Option.SPECIAL_SLAB; i++) {
                            specialChances.put(index, 1);
                            index++;
                        }
                        for (int i = 0; i < Option.SPECIAL_PANE; i++) {
                            specialChances.put(index, 2);
                            index++;
                        }
                        for (int i = 0; i < Option.SPECIAL_FENCE; i++) {
                            specialChances.put(index, 3);
                            index++;
                        }
                    }

                    int spec = specialChances.get(random.nextInt(specialChances.size()));
                    switch (spec) {
                        case 0: // ice
                            material = Material.PACKED_ICE.createBlockData();
                            gap++;
                            break;
                        case 1: // slab
                            material = Material.SMOOTH_QUARTZ_SLAB.createBlockData();
                            height = Math.min(height, 0);
                            ((Slab) material).setType(Slab.Type.BOTTOM);
                            break;
                        case 2: // pane
                            material = Material.GLASS_PANE.createBlockData();
                            gap -= 0.5;
                            break;
                        case 3:
                            material = Material.OAK_FENCE.createBlockData();
                            height = Math.min(height, 0);
                            gap -= 1;
                            break;
                    }
                }

                Location local = lastSpawn.clone();
                if (local.getBlock().getType() == Material.SMOOTH_QUARTZ_SLAB) {
                    height = Math.min(height, 0);
                }
                if (height > 1) {
                    height = 1;
                }
                if (gap > 4) {
                    gap = 4;
                }
                List<Block> possible = getPossible(gap - height, height);
                if (possible.size() == 0) {
                    lastSpawn = local.clone();
                    return;
                }

                Block chosen = possible.get(random.nextInt(possible.size()));
                setBlock(chosen, material);
                generatedHistory.add(chosen);
                if (generatedHistory.size() > player.blockLead + 5) {
                    generatedHistory.remove();
                }
                new BlockGenerateEvent(chosen, this, player).call();
                lastSpawn = chosen.getLocation().clone();

                if (player.useParticles) {
                    PARTICLE_DATA.setType(Option.PARTICLE_TYPE);

                    Player bukkitPlayer = player.getPlayer();
                    switch (Option.PARTICLE_SHAPE) {
                        case DOT:
                            PARTICLE_DATA.setSpeed(0.4);
                            PARTICLE_DATA.setSize(20);
                            PARTICLE_DATA.setOffsetX(0.5);
                            PARTICLE_DATA.setOffsetY(1);
                            PARTICLE_DATA.setOffsetZ(0.5);
                            Particles.draw(lastSpawn.clone().add(0.5, 1, 0.5), PARTICLE_DATA, bukkitPlayer);
                            break;
                        case CIRCLE:
                            PARTICLE_DATA.setSize(5);
                            Particles.circle(lastSpawn.clone().add(0.5, 0.5, 0.5), PARTICLE_DATA, bukkitPlayer, 1, 25);
                            break;
                        case BOX:
                            PARTICLE_DATA.setSize(1);
                            Particles.box(BoundingBox.of(chosen), player.getPlayer().getWorld(), PARTICLE_DATA, bukkitPlayer, 0.15);
                            break;
                    }
                    player.getPlayer().playSound(lastSpawn.clone(), Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
                }

                if (structureCooldown > 0) {
                    structureCooldown--;
                }
                break;
            case 1:
                if (isNearBorder(lastSpawn.clone().toVector()) && score > 0) {
                    generate(); // generate a normal block
                    return;
                }

                File folder = new File(WITP.getInstance().getDataFolder() + "/schematics/");
                List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.contains("parkour-")));
                File file = null;
                if (files.size() > 0) {
                    boolean passed = true;
                    while (passed) {
                        file = files.get(random.nextInt(files.size()));
                        if (player.difficulty == 0) {
                            player.difficulty = 0.3;
                        }
                        if (Util.getDifficulty(file.getName()) < player.difficulty) {
                            passed = false;
                        }
                    }
                } else {
                    Verbose.error("No structures to choose from!");
                    return;
                }
                Schematic schematic = SchematicCache.getSchematic(file.getName());

                structureCooldown = 20;
                int gapStructure = distanceChances.get(random.nextInt(distanceChances.size())) + 1;

                Location local2 = lastSpawn.clone();
                List<Block> possibleStructure = getPossible(gapStructure, 0);
                if (possibleStructure.size() == 0) {
                    lastSpawn = local2.clone();
                    return;
                }
                Block chosenStructure = possibleStructure.get(random.nextInt(possibleStructure.size()));

                try {
                    structureBlocks = SchematicAdjuster.pasteAdjusted(schematic, chosenStructure.getLocation());
                    waitForSchematicCompletion = true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    reset(true);
                }
                if (structureBlocks == null || structureBlocks.size() == 0) {
                    Verbose.error("0 blocks found in structure!");
                    player.send("&cThere was an error while trying to paste a structure! If you don't want this to happen again, you can disable them in the menu.");
                    reset(true);
                }

                for (Block block : structureBlocks) {
                    if (block.getType() == Material.RED_WOOL) {
                        lastSpawn = block.getLocation();
                        break;
                    }
                }
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

    private void setBlock(Block block, BlockData data) {
        if (data instanceof Fence || data instanceof Wall) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data);
        }
    }

    // Gets all possible parkour locations
    protected List<Block> getPossible(double radius, int dy) {
        List<Block> possible = new ArrayList<>();
        World world = lastSpawn.getWorld();
        Location base = lastSpawn.add(0, dy, 0);
        int y = base.getBlockY();
        double detail = (radius * 8);
        double increment = (2 * Math.PI) / detail;

        double heightGap = dy >= 0 ? Option.HEIGHT_GAP - dy : Option.HEIGHT_GAP - (dy + 1);
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
        lastPlayer = spawn.clone();
        blockSpawn = block.clone();
        lastSpawn = block.clone();
        generate(player.blockLead);
    }

    // Generates in a loop
    public void generate(int amount) {
        for (int i = 0; i < amount; i++) {
            generate();
        }
    }
}