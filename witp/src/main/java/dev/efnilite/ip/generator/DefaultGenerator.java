package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.events.BlockGenerateEvent;
import dev.efnilite.ip.events.PlayerFallEvent;
import dev.efnilite.ip.events.PlayerScoreEvent;
import dev.efnilite.ip.generator.base.DefaultGeneratorBase;
import dev.efnilite.ip.generator.base.GeneratorOption;
import dev.efnilite.ip.menu.SettingsMenu;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.reward.RewardReader;
import dev.efnilite.ip.reward.RewardString;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.SchematicAdjuster;
import dev.efnilite.ip.schematic.SchematicCache;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Version;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Slab;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link ParkourPlayer} has.
 *
 * @author Efnilite
 */
public class DefaultGenerator extends DefaultGeneratorBase {

    private BukkitRunnable task;

    private boolean isSpecial;
    private Material specialType;

    /**
     * The amount of blocks that will trail the player's current index.
     */
    protected int blockTrail = 2;


    /**
     * Whether this generator has been stopped
     */
    protected boolean stopped = false;

    /**
     * Whether the stucture should be deleted on the next jump.
     */
    protected boolean deleteStructure = false;
    protected boolean waitForSchematicCompletion = false;

    /**
     * The most recently spawned block
     */
    protected Location mostRecentBlock;

    /**
     * The last location the player was found standing in
     */
    protected Location lastStandingPlayerLocation;

    /**
     * A list of blocks from the (possibly) spawned structure
     */
    protected List<Block> schematicBlocks = new ArrayList<>();

    /**
     * The count total. This is always bigger (or the same) than the positionIndexPlayer
     */
    protected int positionIndexTotal = 0;

    /**
     * The player's current position index.
     */
    protected int lastPositionIndexPlayer = -1;

    /**
     * A map which stores all blocks and their number values. The first block generated will have a value of 0.
     */
    protected final LinkedHashMap<Block, Integer> positionIndexMap = new LinkedHashMap<>();

    protected static final ParticleData<?> PARTICLE_DATA = new ParticleData<>(Particle.SPELL_INSTANT, null, 10, 0, 0, 0, 0);

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param   session
     *          The session associated with this generator
     */
    public DefaultGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);

        this.mostRecentBlock = player.getLocation().clone();
        this.lastStandingPlayerLocation = mostRecentBlock.clone();
        this.heading = Option.HEADING.get();
    }

    @Override
    public void particles(List<Block> applyTo) {
        if (player.useParticlesAndSound && Version.isHigherOrEqual(Version.V1_9)) {
            PARTICLE_DATA.type(Option.PARTICLE_TYPE.get());

            switch (Option.ParticleShape.valueOf(Option.PARTICLE_SHAPE.get().toUpperCase())) {
                case DOT -> {
                    PARTICLE_DATA.speed(0.4).size(20).offsetX(0.5).offsetY(1).offsetZ(0.5);
                    Particles.draw(mostRecentBlock.clone().add(0.5, 1, 0.5), PARTICLE_DATA, player.getPlayer());
                }
                case CIRCLE -> {
                    PARTICLE_DATA.size(5);
                    Particles.circle(mostRecentBlock.clone().add(0.5, 0.5, 0.5), PARTICLE_DATA, player.getPlayer(), (int) Math.sqrt(applyTo.size()), 25);
                }
                case BOX -> {
                    Location min = new Location(blockSpawn.getWorld(), Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    Location max = new Location(blockSpawn.getWorld(), Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
                    for (Block block : applyTo) {
                        Location loc = block.getLocation();
                        min = Util.min(min, loc);
                        max = Util.max(max, loc);
                    }
                    if (max.getBlockX() == Integer.MIN_VALUE || max.getBlockX() == Integer.MAX_VALUE) { // to not crash the server (lol)
                        return;
                    }
                    PARTICLE_DATA.size(1);
                    Particles.box(BoundingBox.of(max, min), player.getPlayer().getWorld(), PARTICLE_DATA, player.getPlayer(), 0.15);
                }
            }
            player.getPlayer().playSound(mostRecentBlock.clone(), Option.SOUND_TYPE.get(), 4, Option.SOUND_PITCH.get());
        }
    }

    @Override
    public BlockData selectBlockData() {
        return player.getRandomMaterial().createBlockData();
    }

    @Override
    public List<Block> selectBlocks() {
        int height;
        int gap = getRandomChance(distanceChances) + 1;

        int zoneMax = zone.getMaximumPoint().getBlockY();
        int zoneMin = zone.getMinimumPoint().getBlockY();
        int mostRecentY = mostRecentBlock.getBlockY();

        if (mostRecentY > zoneMax) { // 204 > 200
            height = -1;
        } else if (zoneMin > mostRecentY) { // 100 > 99
            height = 1;
        } else {
            height = getRandomChance(heightChances);
        }

        if (isSpecial && specialType != null) {
            switch (specialType) { // adjust for special jumps
                case PACKED_ICE -> // ice
                        gap++;
                case QUARTZ_SLAB -> // slab
                        height = Math.min(height, 0);
                case GLASS_PANE -> // pane
                        gap -= 0.5;
                case OAK_FENCE -> {
                        height = Math.min(height, 0);
                        gap -= 1;
                }
            }
        }

        if (mostRecentBlock.getBlock().getType() == Material.QUARTZ_SLAB) { // slabs can't go higher than one
            height = Math.min(height, 0);
        }

        height = Math.min(height, 1);
        gap = Math.min(gap, 4);

        List<Block> possible = getPossiblePositions(gap - height, height);

        if (possible.isEmpty()) {
            return Collections.emptyList();
        }

        return List.of(possible.get(random.nextInt(possible.size())));
    }

    @Override
    public void score() {
        score++;
        totalScore++;
        checkRewards();

        new PlayerScoreEvent(player).call();
    }

    @Override
    public void fall() {
        new PlayerFallEvent(player).call();
        reset(true);
    }

    @Override
    public void menu() {
        SettingsMenu.open(player);
    }

    @Override
    public void startTick() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    this.cancel();
                    return;
                }

                tick();
            }
        };
        Task.create(IP.getPlugin())
                .repeat(option(GeneratorOption.INCREASED_TICK_ACCURACY) ? 1 : Option.GENERATOR_CHECK.get())
                .execute(task)
                .run();
    }

    /**
     * Starts the check
     */
    @Override
    public void tick() {
        updateTime();
        player.getSession().updateSpectators();
        player.updateScoreboard();
        player.getPlayer().setSaturation(20);

        Location playerLocation = player.getLocation();

        if (playerLocation.getWorld() != playerSpawn.getWorld()) { // sometimes player worlds don't match (somehow)
            player.teleport(playerSpawn);
            return;
        }

        if (lastStandingPlayerLocation.getY() - playerLocation.getY() > 10 && playerSpawn.distance(playerLocation) > 5) { // Fall check
            fall();
            return;
        }

        Block blockBelowPlayer = playerLocation.clone().subtract(0, 1, 0).getBlock(); // Get the block below

        if (blockBelowPlayer.getType() == Material.AIR) {
            return;
        }

        if (schematicBlocks.contains(blockBelowPlayer) && blockBelowPlayer.getType() == Material.RED_WOOL && !deleteStructure) { // Structure deletion check
            for (int i = 0; i < 10; i++) {
                score();
            }
            waitForSchematicCompletion = false;
            schematicCooldown = 20;
            generate(player.blockLead);
            deleteStructure = true;
            return;
        }

        if (!positionIndexMap.containsKey(blockBelowPlayer)) {
            return;
        }
        int currentIndex = positionIndexMap.get(blockBelowPlayer); // current index of the player
        int deltaFromLast = currentIndex - lastPositionIndexPlayer;

        if (deltaFromLast <= 0) { // the player is actually making progress and not going backwards (current index is higher than the previous)
            return;
        }

        if (!stopwatch.hasStarted()) { // start stopwatch when first point is achieved
            stopwatch.start();
        }

        lastStandingPlayerLocation = playerLocation.clone();

        if (Option.ALL_POINTS.get()) { // score handling
            for (int i = 0; i < deltaFromLast; i++) { // score the difference
                score();
            }
        } else {
            score();
        }

        int deltaCurrentTotal = positionIndexTotal - currentIndex; // delta between current index and total
        if (deltaCurrentTotal <= player.blockLead) {
            generate(player.blockLead - deltaCurrentTotal + 1); // generate the remaining amount so it will match
        }
        lastPositionIndexPlayer = currentIndex;

        // delete trailing blocks
        for (Block block : new ArrayList<>(positionIndexMap.keySet())) {
            int index = positionIndexMap.get(block);
            if (currentIndex - index > blockTrail) {
                block.setType(Material.AIR);
                positionIndexMap.remove(block);
            }
        }

        if (deleteStructure) { // deletes the structure if the player goes to the next block (reason why it's last)
            deleteStructure();
        }

        calculateDistance();
    }

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
            if (task == null) {// incomplete setup as task is the last thing to start
                IP.logging().warn("Incomplete joining setup: there has probably been an error somewhere. Please report this error to the developer!");
                IP.logging().warn("You don't have to report this warning.");
            } else {
                task.cancel();
            }
        }

        for (Block block : positionIndexMap.keySet()) {
            block.setType(Material.AIR);
        }

        lastPositionIndexPlayer = 0;
        positionIndexTotal = 0;
        positionIndexMap.clear();

        waitForSchematicCompletion = false;
        player.saveGame();
        deleteStructure();

        if (regenerate) {
            player.getPlayer().teleport(playerSpawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

        int score = this.score;
        String time = this.time;
        String diff = player.calculateDifficultyScore();
        if (player.showFallMessage && regenerate && time != null) {
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
                player.setHighScore(player.name, score, time, diff);
            }
            player.sendTranslated("divider");
            player.sendTranslated("score", Integer.toString(score));
            player.sendTranslated("time", time);
            player.sendTranslated("highscore", Integer.toString(player.highScore));
            player.sendTranslated(message, Integer.toString(number));
            player.sendTranslated("divider");
        } else {
            if (score >= player.highScore) {
                player.setHighScore(player.name, score, time, diff);
            }
        }

        this.score = 0;
        stopwatch.stop();

        if (regenerate) { // generate back the blocks
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
    @Override
    public void generate() {
        if (waitForSchematicCompletion) {
            return;
        }

        int type = getRandomChance(defaultChances); // 0 = normal, 1 = structures, 2 = special
        isSpecial = type == 2; // 1 = yes, 0 = no
        if (isSpecial) {
            type = 0;
        } else {
            type = schematicCooldown == 0 && player.useSchematic ? type : 0;
        }
        if (type == 0) {
            if (isNearingEdge(mostRecentBlock) && score > 0) {
                heading = heading.turnRight(); // reverse heading if close to border
            }

            BlockData selectedBlockData = selectBlockData();

            if (isSpecial && player.useSpecialBlocks) { // if special
                int value = getRandomChance(specialChances);
                switch (value) {
                    case 0 -> // ice
                        selectedBlockData = Material.PACKED_ICE.createBlockData();
                    case 1 -> { // slab
                        selectedBlockData = Material.QUARTZ_SLAB.createBlockData();
                        ((Slab) selectedBlockData).setType(Slab.Type.BOTTOM);
                    }
                    case 2 -> // pane
                        selectedBlockData = Material.WHITE_STAINED_GLASS_PANE.createBlockData();
                    case 3 -> // fence
                        selectedBlockData = Material.OAK_FENCE.createBlockData();
                    default -> {
                        selectedBlockData = Material.STONE.createBlockData();
                        IP.logging().stack("Invalid special block ID " + value, new IllegalArgumentException());
                    }
                }
                specialType = selectedBlockData.getMaterial();
            } else {
                specialType = null;
            }

            List<Block> blocks = selectBlocks();
            if (blocks.isEmpty()) {
                return;
            }

            Block selectedBlock = blocks.get(0);
            setBlock(selectedBlock, selectedBlockData);
            new BlockGenerateEvent(selectedBlock, this, player).call();

            positionIndexMap.put(selectedBlock, positionIndexTotal);
            positionIndexTotal++;

            mostRecentBlock = selectedBlock.getLocation().clone();

            particles(List.of(selectedBlock));

            if (schematicCooldown > 0) {
                schematicCooldown--;
            }
        } else if (type == 1) {
            if (isNearingEdge(mostRecentBlock) && score > 0) {
                generate(); // generate a normal block
                return;
            }

            File folder = new File(IP.getPlugin().getDataFolder() + "/schematics/");
            List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.contains("parkour-")));
            File file = null;
            if (!files.isEmpty()) {
                boolean passed = true;
                while (passed) {
                    file = files.get(random.nextInt(files.size()));
                    if (player.schematicDifficulty == 0) {
                        player.schematicDifficulty = 0.2;
                    }
                    if (Util.getDifficulty(file.getName()) < player.schematicDifficulty) {
                        passed = false;
                    }
                }
            } else {
                IP.logging().error("No structures to choose from!");
                generate(); // generate if no schematic is found
                return;
            }
            Schematic schematic = SchematicCache.getSchematic(file.getName());

            schematicCooldown = 20;
            List<Block> blocks = selectBlocks();
            if (blocks.isEmpty()) {
                return;
            }

            Block selectedBlock = blocks.get(0);

            try {
                schematicBlocks = SchematicAdjuster.pasteAdjusted(schematic, selectedBlock.getLocation());
                waitForSchematicCompletion = true;
            } catch (IOException ex) {
                IP.logging().stack("There was an error while trying to paste schematic " + schematic.getName(),
                        "delete this file and restart the server", ex);
                reset(true);
                return;
            }

            if (schematicBlocks == null || schematicBlocks.isEmpty()) {
                IP.logging().error("0 blocks found in structure!");
                player.send("&cThere was an error while trying to paste a structure! If you don't want this to happen again, you can disable them in the menu.");
                reset(true);
                return;
            }

            for (Block schematicBlock : schematicBlocks) {
                if (schematicBlock.getType() == Material.RED_WOOL) {
                    mostRecentBlock = schematicBlock.getLocation();
                    break;
                }
            }
        }
    }

    /**
     * Generates a specific amount of blocks ahead of the player
     *
     * @param   amount
     *          The amount
      */
    public void generate(int amount) {
        for (int i = 0; i < amount; i++) {
            generate();
        }
    }

    protected int getRandomChance(HashMap<Integer, Integer> map) {
        List<Integer> keys = new ArrayList<>(map.keySet());
        if (keys.isEmpty()) {
            calculateChances();
            return 1;
        }
        int index = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        return map.get(index);
    }

    /**
     * Checks a player's rewards and gives them if necessary
     */
    public void checkRewards() {
        // if disabled dont continue
        if (!RewardReader.REWARDS_ENABLED.get() && score > 0) {
            return;
        }

        // check generic score rewards
        List<RewardString> strings = RewardReader.SCORE_REWARDS.get(score);
        if (strings != null) {
            strings.forEach(s -> s.execute(player));
        }

        // gets the correct type of score to check based on the config option
        int typeToCheck = Option.REWARDS_USE_TOTAL_SCORE.get() ? totalScore : score;
        for (int interval : RewardReader.INTERVAL_REWARDS.keySet()) {
            if (typeToCheck % interval == 0) {
                strings = RewardReader.INTERVAL_REWARDS.get(interval);
                strings.forEach(s -> s.execute(player));
            }
        }

        strings = RewardReader.ONE_TIME_REWARDS.get(score);
        if (strings != null && !player.collectedRewards.contains(Integer.toString(score))) {
            strings.forEach(s -> s.execute(player));
            player.collectedRewards.add(Integer.toString(score));
        }
    }

    protected void deleteStructure() {
        for (Block block : schematicBlocks) {
            block.setType(Material.AIR);
        }

        schematicBlocks.clear();
        deleteStructure = false;
        schematicCooldown = 20;
    }

    protected void setBlock(Block block, BlockData data) {
        if (data instanceof Fence || data instanceof GlassPane) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data, false);
        }
    }

    /**
     * Gets all possible locations from a point with a specific radius and delta y value.
     *
     * How it works with example radius of 2 and example delta y of -1:
     * - last spawn location gets lowered by 1 block
     * - detail becomes 2 * 8 = 16, so it should go around the entire of the circle in 16 steps
     * - increment using radians, depends on the detail (2pi / 16)
     *
     * @param   radius
     *          The radius
     *
     * @param   dy
     *          The y that should be added to the last spawned block to update the searching position
     *
     * @return a list of possible blocks (contains copies of the same block)
     */
    protected List<Block> getPossiblePositions(double radius, double dy) {
        List<Block> possible = new ArrayList<>();

        World world = mostRecentBlock.getWorld();
        Location base = mostRecentBlock.add(0, dy, 0); // adds y to the last spawned block
        base.add(0.5, 0, 0.5);
        radius -= 0.5;

        int y = base.getBlockY();

        // the distance, adjusted to the height (dy)
        double heightGap = dy >= 0 ? Option.HEIGHT_GAP.getAsDouble() - dy : Option.HEIGHT_GAP.getAsDouble() - (dy + 1);

        // the range in which it should check for blocks (max 180 degrees, min 90 degrees)
        double range = option(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? Math.PI * 0.5 : Math.PI;

        double[] bounds = getBounds(heading, range);
        double startBound = bounds[0];
        double limitBound = bounds[1];

        double detail = radius * 4; // how many times it should check
        double increment = range / detail; // 180 degrees / amount of times it should check = the increment

        if (radius > 1) {
            startBound += 1.5 * increment; // remove blocks on the same axis
            limitBound -= 1.5 * increment;
        } else if (radius < 1) {
            radius = 1;
        }

        for (int progress = 0; progress < detail; progress++) {
            double angle = startBound + progress * increment;
            if (angle > limitBound) {
                break;
            }
            double x = base.getX() + (radius * Math.cos(angle));
            double z = base.getZ() + (radius * Math.sin(angle));
            Block block = new Location(world, x, y, z).getBlock();

            if (block.getLocation().distance(base) <= heightGap
                    && !possible.contains(block)) { // prevents duplicates
                possible.add(block);
            }
        }

        return possible;
    }

    private double[] getBounds(Direction direction, double range) {
        // todo fix
        return switch (direction) { // cos/sin system works clockwise with north on top, explanation: https://imgur.com/t2SFWc9
            default -> // east
                    // - 1/2 pi to 1/2 pi
                    new double[]{-0.5 * range, 0.5 * range};
            case WEST ->
                    // 1/2 pi to -1/2 pi
                    new double[]{0.5 * range, -0.5 * range};
            case NORTH ->
                    // pi to 0
                    new double[]{range, 0};
            case SOUTH ->
                    // 0 to pi
                    new double[]{0, range};
        };
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
        lastStandingPlayerLocation = spawn.clone();
        blockSpawn = block.clone();
        mostRecentBlock = block.clone();
        generate(player.blockLead + 1);
    }

    public int getTotalScore() {
        return totalScore;
    }
}