package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.api.event.ParkourBlockGenerateEvent;
import dev.efnilite.ip.api.event.ParkourFallEvent;
import dev.efnilite.ip.api.event.ParkourSchematicGenerateEvent;
import dev.efnilite.ip.api.event.ParkourScoreEvent;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Probs;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link ParkourPlayer} has.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourGenerator {

    /**
     * The amount of blocks that trail behind the player.
     */
    private static final int BLOCK_TRAIL = 2;

    /**
     * This generator's score
     */
    public int score = 0;

    /**
     * The zone in which the parkour can take place. (playable area)
     */
    public final Location[] zone;

    /**
     * The direction of the parkour
     */
    public Vector heading = Option.HEADING.clone();

    /**
     * Generator options
     */
    public final List<GeneratorOption> generatorOptions;

    /**
     * The {@link Session} associated with this Generator.
     */
    public final Session session;

    /**
     * This Generator's {@link Profile}.
     *
     * @see Profile
     */
    public final Profile profile = new Profile();

    /**
     * The island instance.
     */
    public final Island island;

    /**
     * The player
     */
    public ParkourPlayer player;

    /**
     * The chances of which distance the jump should have
     */
    public final HashMap<Integer, Double> distanceChances = new HashMap<>();

    /**
     * The chances of which height the jump should have
     */
    public final HashMap<Integer, Double> heightChances = new HashMap<>();

    /**
     * The chances of which type of special jump
     */
    public final HashMap<BlockData, Double> specialChances = new HashMap<>();

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    public final HashMap<JumpType, Double> defaultChances = new HashMap<>();

    /**
     * The total score achieved in this Generator instance
     */
    public int totalScore = 0;

    /**
     * The schematic cooldown
     */
    public int schematicCooldown = Option.SCHEMATIC_COOLDOWN;

    /**
     * Where the player spawns on reset
     */
    public Location playerSpawn;

    /**
     * Where blocks from schematics spawn
     */
    public Location blockSpawn;

    /**
     * The task used in checking the player's current location
     */
    public BukkitTask task;

    /**
     * Whether this generator has been stopped
     */
    public boolean stopped = false;

    /**
     * Instant when run started.
     */
    public Instant start;

    /**
     * Whether the schematic should be deleted on the next jump.
     */
    protected boolean deleteSchematic = false;
    protected boolean waitForSchematicCompletion = false;

    /**
     * The last location the player was found standing in
     */
    protected Location lastStandingPlayerLocation;

    /**
     * A list of blocks from the (possibly) spawned structure
     */
    protected List<Block> schematicBlocks = new ArrayList<>();

    /**
     * The player's current position index.
     */
    protected int lastPositionIndexPlayer = -1;

    protected List<Block> history = new ArrayList<>();

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param session The session associated with this generator
     */
    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = Arrays.asList(generatorOptions);

        player = session.getPlayers().get(0);
        island = new Island(session, Schematics.CACHE.get("spawn-island"));
        zone = WorldDivider.toSelection(session);

        calculateChances();
    }

    /**
     * Calculates all chances for every variable
     */
    protected void calculateChances() {
        // default
        defaultChances.clear();

        defaultChances.put(JumpType.DEFAULT, Option.NORMAL);
        defaultChances.put(JumpType.SCHEMATIC, Option.SCHEMATICS);
        defaultChances.put(JumpType.SPECIAL, Option.SPECIAL);

        // height
        heightChances.clear();

        heightChances.put(1, Option.NORMAL_UP);
        heightChances.put(0, Option.NORMAL_LEVEL);
        heightChances.put(-1, Option.NORMAL_DOWN);
        heightChances.put(-2, Option.NORMAL_DOWN2);

        // distance
        distanceChances.clear();

        distanceChances.put(1, Option.NORMAL_ONE_BLOCK);
        distanceChances.put(2, Option.NORMAL_TWO_BLOCK);
        distanceChances.put(3, Option.NORMAL_THREE_BLOCK);
        distanceChances.put(4, Option.NORMAL_FOUR_BLOCK);

        // special
        specialChances.clear();

        specialChances.put(Material.PACKED_ICE.createBlockData(), Option.SPECIAL_ICE);
        specialChances.put(Material.SMOOTH_QUARTZ_SLAB.createBlockData("[type=bottom]"), Option.SPECIAL_SLAB);
        specialChances.put(Material.GLASS_PANE.createBlockData(), Option.SPECIAL_PANE);
        specialChances.put(Material.OAK_FENCE.createBlockData(), Option.SPECIAL_FENCE);
    }

    protected void particles(List<Block> blocks) {
        if (!profile.get("particles").asBoolean()) {
            return;
        }

        ParticleData<?> data = Option.PARTICLE_DATA;
        List<Location> locations = blocks.stream().map(Block::getLocation).toList();
        Location max = locations.stream().reduce(Locations::max).orElseThrow();
        Location min = locations.stream().reduce(Locations::min).orElseThrow();
        Location center = min.clone().add(max.clone().subtract(min));

        // display particle
        switch (Option.PARTICLE_SHAPE) {
            case DOT -> Particles.draw(center.add(0.5, 1, 0.5), data.speed(0.4).size(20).offsetX(0.5).offsetY(1).offsetZ(0.5));
            case CIRCLE -> Particles.circle(center.add(0.5, 0.5, 0.5), data.size(5), (int) Math.sqrt(blocks.size()), 20);
            case BOX -> Particles.box(BoundingBox.of(max, min), player.player.getWorld(), data.size(1), 0.2);
        }
    }

    protected void sound(List<Block> blocks) {
        if (!profile.get("sound").asBoolean()) {
            return;
        }

        // play sound
        for (ParkourPlayer viewer : session.getPlayers()) {
            viewer.player.playSound(blocks.get(0).getLocation(), Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
        }
        for (ParkourSpectator viewer : session.getSpectators()) {
            viewer.player.playSound(blocks.get(0).getLocation(), Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
        }
    }

    protected BlockData selectBlockData() {
        String style = profile.get("style").value();

        Material material = Registry.getTypeFromStyle(style).get(style);

        // if found style is null, get the first registered style to prevent big boy errors
        if (material == null) {
            String newStyle = new ArrayList<>(Registry.getStyleTypes().get(0).styles.keySet()).get(0);

            profile.set("style", newStyle);

            return selectBlockData();
        }
        return material.createBlockData();
    }

    protected List<Block> selectBlocks() {
        int dy = Probs.random(heightChances);
        int gap = Probs.random(distanceChances);

        if (dy > 0 && gap < 2) { // prevent blocks from spawning on top of each other
            gap = 2;
        }

        return List.of(selectNext(getLatest(), gap, dy));
    }

    // Selects the next block that will continue the parkour.
    // This is done by choosing a random value for the sideways movement.
    // Based on this sideways movement, a value for forward movement will be chosen.
    // This is done to ensure players are able to complete the jump.
    private Block selectNext(Block current, int range, int dy) {
        // calculate the player's location as parameter form to make it easier to detect
        // when a player is near the edge of the playable area
        double[][] progress = calculateParameterization();

        // calculate recommendations for new heading
        List<Vector> recommendations = updateHeading(progress);

        if (!recommendations.isEmpty()) {
            heading = new Vector(0, 0, 0);

            // add all recommendations to heading.
            // this will allow the heading to become diagonal in case it reaches a corner:
            // if north and east are recommended, the heading of the parkour will go north-east.
            for (Vector recommendation : recommendations) {
                heading.add(recommendation);
            }
        }

        dy = updateHeight(progress, dy);

        switch (getLatest().getType()) {
            case PACKED_ICE -> range += 0.5;
            case SMOOTH_QUARTZ_SLAB -> dy = 0;
            case WHITE_STAINED_GLASS_PANE -> range -= 0.5;
        }

        // the adjusted dy, used to get the updated max range
        int ady = dy;

        // change coefficient of line if dy is below 0
        if (dy < 0) {
            ady = (int) Math.ceil(0.5 * dy); // use ceil since ceiling of negative is closer to 0
        }

        // the max range, adjusted to the difference in height
        int adjustedRange = range - ady;

        int ds = 0;
        if (-adjustedRange + 1 < adjustedRange) { // prevent illegal random args

            // make sure df is always 1 by making sure adjustedRange > ds
            // +1 to follow exclusivity of upper bound
            ds = ThreadLocalRandom.current().nextInt(-adjustedRange + 1, adjustedRange);
        }

        // if selection angle is reduced, halve the current sideways step
        if (generatorOptions.contains(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE)) {
            if (ds > 1) {
                ds = 1;
            } else if (ds < -1) {
                ds = -1;
            }
        }

        // delta forwards
        int df = adjustedRange - Math.abs(ds);

        Vector offset = new Vector(df, dy, ds);

        // update current loc
        Location clone = current.getLocation();

        // add all offsets to a vector and rotate it to match current direction
        offset.rotateAroundY(Option.HEADING.clone().angle(heading));

        clone.add(offset);

        return clone.getBlock();
    }

    // Calculates the player's position in a parameter form, to make it easier to detect when the player is near the edge of the border.
    // Returns a 2-dimensional array where the first array index is used to select the x, y and z (0, 1 and 2 respectively).
    // This returns an array where the first index is tx and second index is borderMarginX (see comments below for explanation).
    private double[][] calculateParameterization() {
        Location min = zone[0];
        Location max = zone[1];

        // the total dimensions
        double dx = max.getX() - min.getX();
        double dy = max.getY() - min.getY();
        double dz = max.getZ() - min.getZ();

        // the relative x, y and z coordinates
        // relative being from the min point of the selection zone
        double relativeX = getLatest().getX() - min.getX();
        double relativeY = getLatest().getY() - min.getY();
        double relativeZ = getLatest().getZ() - min.getZ();

        // get progress along axes
        // tx = 0 means that the player is at the same x coordinate as the min point (origin)
        // tx = 1 means that the player is at the same x coordinate as the max point
        // everything between is the progress between these two points, relatively speaking
        double tx = relativeX / dx;
        double ty = relativeY / dy;
        double tz = relativeZ / dz;

        // the minimum distance allowed to the border
        // max block jump distance is 5, so 6 is the max safe distance
        double safeDistance = 6;

        // the margin until the border
        // if tx < borderMarginX, it means the x coordinate is within 'safeDistance' blocks of the border
        double borderMarginX = safeDistance / dx;
        double borderMarginY = (0.5 * safeDistance) / dy;
        double borderMarginZ = safeDistance / dz;

        return new double[][] {{tx, borderMarginX}, {ty, borderMarginY}, {tz, borderMarginZ}};
    }

    // Updates the heading to make sure it avoids the border of the selected zone.
    // When the most recent block is detected to be within a 5-block radius of the border,
    // the heading will automatically be turned around to ensure that the edge does not get
    // destroyed.
    private List<Vector> updateHeading(double[][] progress) {
        // get x values from progress array
        double tx = progress[0][0];
        double borderMarginX = progress[0][1];

        // get z values from progress array
        double tz = progress[2][0];
        double borderMarginZ = progress[2][1];

        List<Vector> directions = new ArrayList<>();
        // check border
        if (tx < borderMarginX) {
            directions.add(new Vector(1, 0, 0));
            // x should increase
        } else if (tx > 1 - borderMarginX) {
            directions.add(new Vector(-1, 0, 0));
            // x should decrease
        }

        if (tz < borderMarginZ) {
            directions.add(new Vector(0, 0, 1));
            // z should increase
        } else if (tz > 1 - borderMarginZ) {
            directions.add(new Vector(0, 0, -1));
            // z should decrease
        }

        return directions;
    }

    // Updates the height to make sure the player doesn't go below the playable zone.
    // If the current height is fine, it will return the value of parameter currentHeight.
    // If the current height is within the border margin, it will return a value (1 or -1)
    // to make sure the player doesn't go below this value
    private int updateHeight(double[][] progress, int currentHeight) {
        double ty = progress[1][0];
        double borderMarginY = progress[1][1];

        if (ty < borderMarginY) {
            return 1;
            // y should increase
        } else if (ty > 1 - borderMarginY) {
            return -1;
            // y should decrease
        } else {
            return currentHeight;
        }
    }

    protected void score() {
        score++;
        totalScore++;

        checkRewards();
        new ParkourScoreEvent(player).call();
    }

    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || score == 0 || !(getMode() instanceof DefaultMode)) {
            return;
        }

        // check generic score rewards
        if (Rewards.SCORE_REWARDS.containsKey(score)) {
            Rewards.SCORE_REWARDS.get(score).forEach(s -> s.execute(player));
        }

        // gets the correct type of score to check based on the config option
        int intervalScore = Option.REWARDS_USE_TOTAL_SCORE ? totalScore : score;
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (intervalScore % interval != 0) {
                continue;
            }

            Rewards.INTERVAL_REWARDS.get(interval).forEach(s -> s.execute(player));
        }

        if (Rewards.ONE_TIME_REWARDS.containsKey(score) && !player.collectedRewards.contains(Integer.toString(score))) {
            Rewards.ONE_TIME_REWARDS.get(score).forEach(s -> s.execute(player));
            player.collectedRewards.add(Integer.toString(score));
        }
    }

    protected void fall() {
        new ParkourFallEvent(player).call();
        reset(true);
    }

    public void menu() {
        Menus.PARKOUR_SETTINGS.open(player);
    }

    public void startTick() {
        task = Task.create(IP.getPlugin())
                .repeat(generatorOptions.contains(GeneratorOption.INCREASED_TICK_ACCURACY) ? 1 : Option.GENERATOR_CHECK)
                .execute(this::tick)
                .run();
    }

    /**
     * Starts the check
     */
    protected void tick() {
        if (stopped) {
            task.cancel();
            return;
        }

        for (ParkourPlayer other : session.getPlayers()) {
            updateVisualTime(other, other.selectedTime);
            other.updateScoreboard(this);
            other.player.setSaturation(20);
        }

        for (ParkourSpectator spectator : session.getSpectators()) {
            spectator.update();
        }

        if (player.getLocation().subtract(lastStandingPlayerLocation).getY() < -10) { // fall check
            fall();
            return;
        }

        Location belowPlayer = player.getLocation().subtract(0, 1, 0);
        Block blockBelowPlayer = belowPlayer.getBlock(); // Get the block below

        if (blockBelowPlayer.getType() == Material.AIR) {
            if (belowPlayer.subtract(0, 0.5, 0).getBlock().getType() == Material.AIR) {
                return;
            }
            blockBelowPlayer = belowPlayer.getBlock();
        }

        if (schematicBlocks.contains(blockBelowPlayer) && blockBelowPlayer.getType() == Material.RED_WOOL && !deleteSchematic) { // Structure deletion check
            for (int i = 0; i < calculateDifficultyScore() * 15; i++) {
                score();
            }
            waitForSchematicCompletion = false;
            schematicCooldown = Option.SCHEMATIC_COOLDOWN;
            generate(profile.get("blockLead").asInt());
            deleteSchematic = true;
            return;
        }

        if (!history.contains(blockBelowPlayer)) {
            return; // player is on an unknown block
        }

        int currentIndex = history.indexOf(blockBelowPlayer); // current index of the player
        int deltaFromLast = currentIndex - lastPositionIndexPlayer;

        if (deltaFromLast <= 0) { // the player is actually making progress and not going backwards (current index is higher than the previous)
            return;
        }

        lastStandingPlayerLocation = player.getLocation();

        int blockLead = profile.get("blockLead").asInt();

        int deltaCurrentTotal = history.size() - currentIndex; // delta between current index and total
        if (deltaCurrentTotal <= blockLead) {
            generate(blockLead - deltaCurrentTotal); // generate the remaining amount so it will match
        }
        lastPositionIndexPlayer = currentIndex;

        // delete trailing blocks
        for (int idx = 0; idx < history.size(); idx++) {
            Block block = history.get(idx);

            if (currentIndex - idx > BLOCK_TRAIL) {
                block.setType(Material.AIR);
            }
        }

        if (deleteSchematic) { // deletes the structure if the player goes to the next block (reason why it's last)
            deleteStructure();
        }

        for (int i = 0; i < (Option.ALL_POINTS ? deltaFromLast : 1); i++) { // score the difference
            score();
        }

        if (start == null) { // start stopwatch when first point is achieved
            start = Instant.now();
        }
    }

    // updates the player time
    private void updateVisualTime(ParkourPlayer player, int selectedTime) {
        int newTime = 18000 + selectedTime;
        if (newTime >= 24000) {
            newTime -= 24000;
        }

        player.player.setPlayerTime(newTime, false);
    }

    /**
     * Resets the parkour. If regenerate is false, this generator is stopped and the island is destroyed.
     *
     * @param regenerate True if parkour should regenerate, false if not.
     */
    public void reset(boolean regenerate) {
        stopped = !regenerate;

        if (!regenerate && task == null) {
            IP.logging().warn("## Incomplete joining setup.");
            IP.logging().warn("## There has probably been an error somewhere. Please report this error!");
            IP.logging().warn("## You don't have to report this warning.");
        }

        lastPositionIndexPlayer = 0;
        history.forEach(block -> block.setType(Material.AIR));
        history.clear();

        waitForSchematicCompletion = false;
        deleteStructure();

        Leaderboard leaderboard = getMode().getLeaderboard();
        int record = leaderboard != null ? leaderboard.get(player.getUUID()).score() : 0;
        String time = getTime();

        if (profile.get("showFallMessage").asBoolean()) {
            String message;
            int number = 0;

            if (score == record) {
                message = "settings.parkour_settings.items.fall_message.formats.tied";
            } else if (score > record) {
                number = score - record;
                message = "settings.parkour_settings.items.fall_message.formats.beat";
            } else {
                number = record - score;
                message = "settings.parkour_settings.items.fall_message.formats.miss";
            }

            for (ParkourPlayer players : session.getPlayers()) {
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
                players.sendTranslated("settings.parkour_settings.items.fall_message.score", Integer.toString(score));
                players.sendTranslated("settings.parkour_settings.items.fall_message.time", time);
                players.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record));
                players.sendTranslated(message, Integer.toString(number));
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            }
        }

        if (leaderboard != null && score > record) {
            for (ParkourPlayer player : session.getPlayers()) {
                leaderboard.put(player.getUUID(), new Score(player.getName(), getTime(), Double.toString(calculateDifficultyScore()).substring(0, 3), score));
            }
        }

        score = 0;
        start = null;

        if (regenerate) { // generate back the blocks
            player.teleport(playerSpawn);
            generateFirst(playerSpawn, blockSpawn);
            return;
        }

        island.destroy();

        for (ParkourSpectator spectator : session.getSpectators()) {
            ParkourUser.register(spectator.player);
        }
    }

    private void deleteStructure() {
        schematicBlocks.forEach(block -> block.setType(Material.AIR));
        schematicBlocks.clear();

        deleteSchematic = false;
        schematicCooldown = Option.SCHEMATIC_COOLDOWN;
    }

    /**
     * Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
     * the player to achieve this score using their settings.
     */
    public double calculateDifficultyScore() {
        double score = 0;

        if (profile.get("useSpecialBlocks").asBoolean())                score += 0.5;
        if (profile.get("useStructure").asBoolean()) {
            if (profile.get("schematicDifficulty").asDouble() <= 0.25)  score += 0.2;
            if (profile.get("schematicDifficulty").asDouble() <= 0.5)   score += 0.3;
            if (profile.get("schematicDifficulty").asDouble() <= 0.75)  score += 0.4;
            if (profile.get("schematicDifficulty").asDouble() <= 1.0)   score += 0.5;
        }

        return score;
    }

    /**
     * Generates a specific amount of blocks ahead of the player
     *
     * @param amount The amount
     */
    public void generate(int amount) {
        for (int i = 0; i < amount + 1; i++) {
            generate();
        }
    }

    /**
     * Generates the first few blocks (which come off the spawn island)
     *
     * @param spawn The spawn of the player
     * @param block The location used to begin the parkour of off
     */
    public void generateFirst(Location spawn, Location block) {
        playerSpawn = spawn;
        lastStandingPlayerLocation = spawn;
        blockSpawn = block;
        history.add(blockSpawn.getBlock());

        generate(profile.get("blockLead").asInt());
    }

    /**
     * Generates the next parkour block, choosing between structures and normal jumps.
     * If it's a normal jump, it will get a random distance between them and whether it
     * goes up or not.
     * <p>
     * Note: please be cautious when messing about with parkour generation, since even simple changes
     * could break the entire plugin
     */
    public void generate() {
        if (waitForSchematicCompletion) {
            return;
        }

        JumpType jump = schematicCooldown <= 0 ? Probs.random(defaultChances) : JumpType.DEFAULT;

        switch (jump) {
            case DEFAULT, SPECIAL -> {
                List<Block> blocks = selectBlocks();

                if (blocks.isEmpty()) {
                    IP.logging().stack("Error while trying to generate parkour", new NoSuchElementException("No blocks to generate found"));
                    return;
                }

                for (Block block : blocks) {
                    BlockData data = jump == JumpType.SPECIAL ? Probs.random(specialChances) : selectBlockData();

                    block.setBlockData(data, data instanceof Fence || data instanceof GlassPane);
                }

                new ParkourBlockGenerateEvent(blocks, this, player).call();

                particles(blocks);
                sound(blocks);

                history.addAll(blocks);
                schematicCooldown--;
            }
            case SCHEMATIC -> {
                List<String> schematics = new ArrayList<>(Schematics.CACHE.keySet());

                double difficulty = profile.get("schematicDifficulty").asDouble();

                Schematic schematic = Schematics.CACHE.get(Colls.random(schematics.stream()
                        .filter(name -> name.contains("parkour-") && getDifficulty(name) <= difficulty)
                        .toList()));

                schematicBlocks = rotatedPaste(schematic, selectBlocks().get(0).getLocation());

                new ParkourSchematicGenerateEvent(schematic, this, player).call();

                if (schematicBlocks.isEmpty()) {
                    IP.logging().stack("Error while trying to paste schematic %s".formatted(schematic.getFile().getName()), new NoSuchElementException("No schematic blocks found"));
                    return;
                }

                schematicCooldown = Option.SCHEMATIC_COOLDOWN;
                waitForSchematicCompletion = true;
            }
        }
    }

    private @NotNull List<Block> rotatedPaste(Schematic schematic, Location location) {
        if (schematic == null || location == null) {
            return Collections.emptyList();
        }

        Optional<Vector> optionalStart = schematic.getVectorBlockMap().entrySet().stream()
                .filter(e -> e.getValue().getMaterial() == Material.LIME_WOOL)
                .map(Map.Entry::getKey)
                .findAny();

        Optional<Vector> optionalEnd = schematic.getVectorBlockMap().entrySet().stream()
                .filter(e -> e.getValue().getMaterial() == Material.RED_WOOL)
                .map(Map.Entry::getKey)
                .findAny();

        if (optionalStart.isEmpty()) {
            IP.logging().stack("Error while trying to find start of schematic", "check if you placed a lime wool block");
            return Collections.emptyList();
        }
        if (optionalEnd.isEmpty()) {
            IP.logging().stack("Error while trying to find end of schematic", "check if you placed a red wool block");
            return Collections.emptyList();
        }

        Vector start = optionalStart.get();
        Vector end = optionalEnd.get();
        Vector startToEnd = end.clone().subtract(start);

        // the angle between heading and normalized direction of schematic, snapped to 90 deg angles
        double anglePer90Deg = angleInY(heading,
                Math.abs(startToEnd.getX()) > Math.abs(startToEnd.getZ())  // normalized direction of schematic snapped to 90 deg angles
                    ? new Vector(Math.signum(startToEnd.getX()), 0, 0)   // x > z
                    : new Vector(0, 0, Math.signum(startToEnd.getZ()))); // z > x

        Location rotatedStart = location.clone().subtract(start.clone().rotateAroundY(anglePer90Deg));
        Vector rotatedStartToEnd = startToEnd.clone().rotateAroundY(anglePer90Deg);

        history.add(location.clone().add(rotatedStartToEnd).getBlock());
        return schematic.paste(rotatedStart, new Vector(0, anglePer90Deg, 0)); // only yaw
    }

    private double angleInY(Vector a, Vector b) {
        double det = a.getX() * b.getZ() - a.getZ() * b.getX();
        return Math.atan2(det, a.dot(b));
    }

    private Block getLatest() {
        return history.get(history.size() - 1);
    }

    private double getDifficulty(String fileName) {
        return Config.SCHEMATICS.getDouble("difficulty.%s".formatted(fileName.split("[-.]")[1]));
    }

    /**
     * @return The current duration of the run.
     */
    public String getTime() {
        Date date = new Date(start != null ? Duration.between(start, Instant.now()).toMillis() : 0);

        return Score.TIME_FORMAT.format(date);
    }

    /**
     * @return This generator's mode.
     */
    public Mode getMode() {
        return Modes.DEFAULT;
    }

    private enum JumpType {
        DEFAULT, SCHEMATIC, SPECIAL
    }
}