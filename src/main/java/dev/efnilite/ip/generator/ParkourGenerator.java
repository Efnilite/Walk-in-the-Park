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
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.style.Style;
import dev.efnilite.ip.world.Divider;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.schematic.Schematic;
import dev.efnilite.vilib.util.Colls;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Probs;
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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    public static final int BLOCK_TRAIL = 2;

    /**
     * This generator's score
     */
    public int score = 0;

    /**
     * The total score achieved in this Generator instance
     */
    public int totalScore = 0;

    /**
     * The schematic cooldown
     */
    public int schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");

    /**
     * Whether this generator has been stopped
     */
    public boolean stopped = false;

    /**
     * The zone in which the parkour can take place. (playable area)
     */
    public Location[] zone;

    /**
     * The player
     */
    public ParkourPlayer player;

    /**
     * The task used in checking the player's current location
     */
    public BukkitTask task;

    /**
     * Where blocks from schematics spawn
     */
    public Location blockSpawn;

    /**
     * Where the player spawns on reset
     */
    public Location playerSpawn;

    /**
     * Instant when run started.
     */
    public Instant start;

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
     */
    public final Profile profile = new Profile();

    /**
     * The island instance.
     */
    public final Island island;

    /**
     * The chances of which distance the jump should have
     */
    public final Map<Integer, Double> distanceChances = new HashMap<>();

    /**
     * The chances of which height the jump should have
     */
    public final Map<Integer, Double> heightChances = new HashMap<>();

    /**
     * The chances of which type of special jump
     */
    public final Map<BlockData, Double> specialChances = new HashMap<>();

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    public final Map<JumpType, Double> defaultChances = new HashMap<>();

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

    /**
     * The history of generated blocks. The most recently generated block is the last item in the list.
     */
    protected List<Block> history = new ArrayList<>();

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param session          The session.
     * @param schematic        The schematic to use for the spawn island.
     * @param generatorOptions The options.
     */
    public ParkourGenerator(@NotNull Session session, @Nullable Schematic schematic, GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = Arrays.asList(generatorOptions);

        player = session.getPlayers().get(0);
        island = new Island(session, schematic);
        zone = Divider.toSelection(session);

        calculateChances();
    }

    /**
     * Creates a new ParkourGenerator instance.
     *
     * @param session          The session.
     * @param generatorOptions The options.
     */
    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this(session, Schematics.CACHE.get("spawn-island"), generatorOptions);
    }

    /**
     * Ensures generator preferences in profile can't be overridden by the player changing settings.
     */
    public void overrideProfile() { }

    /**
     * Calculates all chances for every variable.
     * Modification is possible in the generator constructor or through external map changes.
     */
    protected void calculateChances() {
        defaultChances.clear();
        defaultChances.put(JumpType.DEFAULT, Option.TYPE_NORMAL);
        defaultChances.put(JumpType.SCHEMATIC, Option.TYPE_SCHEMATICS);
        defaultChances.put(JumpType.SPECIAL, Option.TYPE_SPECIAL);

        heightChances.clear();
        heightChances.put(1, Option.NORMAL_HEIGHT_1);
        heightChances.put(0, Option.NORMAL_HEIGHT_0);
        heightChances.put(-1, Option.NORMAL_HEIGHT_NEG1);
        heightChances.put(-2, Option.NORMAL_HEIGHT_NEG2);

        distanceChances.clear();
        distanceChances.put(1, Option.NORMAL_DISTANCE_1);
        distanceChances.put(2, Option.NORMAL_DISTANCE_2);
        distanceChances.put(3, Option.NORMAL_DISTANCE_3);
        distanceChances.put(4, Option.NORMAL_DISTANCE_4);

        specialChances.clear();
        specialChances.put(Material.PACKED_ICE.createBlockData(), Option.SPECIAL_ICE);
        specialChances.put(Material.SMOOTH_QUARTZ_SLAB.createBlockData("[type=bottom]"), Option.SPECIAL_SLAB);
        specialChances.put(Material.GLASS_PANE.createBlockData(), Option.SPECIAL_PANE);
        specialChances.put(Material.OAK_FENCE.createBlockData(), Option.SPECIAL_FENCE);
    }

    /**
     * Generates particles around blocks.
     *
     * @param blocks The blocks.
     */
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

    /**
     * Generates sound around blocks.
     *
     * @param blocks The blocks.
     */
    protected void sound(List<Block> blocks) {
        if (!profile.get("sound").asBoolean()) {
            return;
        }

        // play sound
        getPlayers().forEach(viewer -> viewer.player.playSound(blocks.get(0).getLocation(), Option.SOUND_TYPE, Option.SOUND_VOLUME, Option.SOUND_PITCH));
        getSpectators().forEach(viewer -> viewer.player.playSound(blocks.get(0).getLocation(), Option.SOUND_TYPE, Option.SOUND_VOLUME, Option.SOUND_PITCH));
    }

    protected BlockData selectBlockData() {
        Style style = Registry.getStyle(profile.get("style").value());

        if (style == null) {
            profile.set("style", Registry.getStyles().stream().findFirst().get().getName());
            return selectBlockData();
        }

        return style.getNext().createBlockData();
    }

    protected List<Block> selectBlocks() {
        int height = Probs.random(heightChances);
        int distance = Probs.random(distanceChances);

        return List.of(selectNext(getLatest(), distance, height));
    }

    // Selects the next block that will continue the parkour.
    // This is done by choosing a random value for the sideways movement.
    // Based on this sideways movement, a value for forward movement will be chosen.
    // This is done to ensure players are able to complete the jump.
    protected Block selectNext(Block current, int distance, int height) {
        JumpDirector director = new JumpDirector(BoundingBox.of(zone[0], zone[1]), getLatest().getLocation().toVector());

        Vector recommendedHeading = director.getRecommendedHeading();

        if (!recommendedHeading.equals(new Vector(0, 0, 0))) {
            heading = recommendedHeading;
        }

        int recommendedHeight = director.getRecommendedHeight();

        if (recommendedHeight != 0) {
            height = recommendedHeight;
        }

        // ensure special is possible
        switch (getLatest().getType()) {
            case SMOOTH_QUARTZ_SLAB -> height = Math.min(height, 0);
            case GLASS_PANE -> distance = Math.min(distance, 3);
        }

        if (height > 0) {
            distance = Math.max(distance - height, 1);
        }

        double mean = 0;
        double standardDeviation = generatorOptions.contains(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? 0.6 : 1;

        int randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(mean, standardDeviation);

        Vector offset = new Vector(distance + 1, height, randomOffset);

        // rotate offset to match heading
        offset.rotateAroundY(angleInY(heading, Option.HEADING.clone()));

        return current.getLocation().add(offset).getBlock();
    }

    protected void score() {
        score++;
        totalScore++;

        checkRewards();
        new ParkourScoreEvent(player).call();
    }

    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || score == 0) {
            return;
        }

        // check generic score rewards
        if (Rewards.SCORE_REWARDS.containsKey(score)) {
            Rewards.SCORE_REWARDS.get(score).forEach(s -> s.execute(player, getMode()));
        }

        // gets the correct type of score to check based on the config option
        int intervalScore = Config.CONFIG.getBoolean("scoring.rewards-use-total-score") ? totalScore : score;
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (intervalScore % interval != 0) {
                continue;
            }

            Rewards.INTERVAL_REWARDS.get(interval).forEach(s -> s.execute(player, getMode()));
        }

        if (Rewards.ONE_TIME_REWARDS.containsKey(score) && !player.collectedRewards.contains(Integer.toString(score))) {
            Rewards.ONE_TIME_REWARDS.get(score).forEach(s -> s.execute(player, getMode()));
            player.collectedRewards.add(Integer.toString(score));
        }
    }

    protected void fall() {
        new ParkourFallEvent(player).call();
        reset(true);
    }

    public void menu(ParkourPlayer player) {
        Menus.PARKOUR_SETTINGS.open(player);
    }

    public void startTick() {
        task = Task.create(IP.getPlugin())
            .repeat(generatorOptions.contains(GeneratorOption.INCREASED_TICK_ACCURACY) ? 1 : Config.GENERATION.getInt("advanced.generator-check"))
            .execute(this::tick)
            .run();
    }

    /**
     * Starts the check
     */
    public void tick() {
        if (stopped) {
            task.cancel();
            return;
        }

        getPlayers().forEach(other -> {
            updateVisualTime(other, other.selectedTime);
            other.updateScoreboard(this);
            other.player.setSaturation(20);
        });

        getSpectators().forEach(ParkourSpectator::update);

        if (player.getLocation().getWorld() != lastStandingPlayerLocation.getWorld()) {
            return;
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
            System.out.println("schematic score call");
            System.out.println(profile.get("schematicDifficulty").asDouble());

            for (int i = 0; i < profile.get("schematicDifficulty").asDouble() * 15; i++) {
                score();
            }

            waitForSchematicCompletion = false;
            schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
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

        for (int i = currentIndex - BLOCK_TRAIL - 1; i >= currentIndex - 4 * BLOCK_TRAIL; i--) {
            // avoid setting beginning block to air
            if (i <= 0) {
                continue;
            }

            history.get(i).setType(Material.AIR);
        }

        deleteSchematic();

        for (int i = 0; i < (Config.CONFIG.getBoolean("scoring.all-points") ? deltaFromLast : 1); i++) { // score the difference
            score();
        }

        if (start == null) { // start stopwatch when first point is achieved
            start = Instant.now();
        }
    }

    // updates the player time
    protected void updateVisualTime(ParkourPlayer player, int selectedTime) {
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
        if (!history.isEmpty()) {
            history.remove(0);
            history.forEach(block -> block.setType(Material.AIR, false));
            history.clear();
        }

        waitForSchematicCompletion = false;
        deleteSchematic = true;
        deleteSchematic();

        Leaderboard leaderboard = getMode().getLeaderboard();
        int record = leaderboard != null ? leaderboard.get(player.getUUID()).score() : 0;

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

            for (ParkourPlayer players : getPlayers()) {
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
                players.sendTranslated("settings.parkour_settings.items.fall_message.score", Integer.toString(score));
                players.sendTranslated("settings.parkour_settings.items.fall_message.time", getFormattedTime());
                players.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record));
                players.sendTranslated(message, Integer.toString(number));
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            }
        }

        if (leaderboard != null && score > record) {
            registerScore(getDetailedTime(), Double.toString(getDifficultyScore()).substring(0, 3), score);
        }

        score = 0;
        start = null;
        heading = Option.HEADING;

        if (regenerate) { // generate back the blocks
            player.teleport(playerSpawn);
            generateFirst(playerSpawn, blockSpawn);
            return;
        }

        island.destroy();

        if (getPlayers().isEmpty()) {
            getSpectators().forEach(spectator -> Modes.DEFAULT.create(spectator.player));
        }
    }

    protected void registerScore(String time, String difficulty, int score) {
        Leaderboard leaderboard = getMode().getLeaderboard();

        if (leaderboard == null) {
            return;
        }

        getPlayers().forEach(player -> leaderboard.put(player.getUUID(), new Score(player.getName(), time, difficulty, score)));
    }

    private void deleteSchematic() {
        if (!deleteSchematic) {
            return;
        }

        schematicBlocks.forEach(block -> block.setType(Material.AIR));
        schematicBlocks.clear();

        deleteSchematic = false;
        schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
    }

    /**
     * Generates the next parkour block or schematic.
     */
    public void generate() {
        if (waitForSchematicCompletion) {
            return;
        }

        Map<JumpType, Double> chances = new HashMap<>(defaultChances);
        if (schematicCooldown > 0 || generatorOptions.contains(GeneratorOption.DISABLE_SCHEMATICS) || profile.get("schematicDifficulty").asDouble() == 0.0 || !schematicBlocks.isEmpty()) {
            chances.remove(JumpType.SCHEMATIC);
        }
        if (!profile.get("useSpecialBlocks").asBoolean()) {
            chances.remove(JumpType.SPECIAL);
        }
        if (chances.isEmpty()) {
            chances.put(JumpType.DEFAULT, 1.0);
        }

        JumpType jump = Probs.random(chances);
        if (jump == JumpType.SCHEMATIC) {
            double difficulty = profile.get("schematicDifficulty").asDouble();

            Schematic schematic = Schematics.CACHE.get(Colls.random(Schematics.CACHE.keySet().stream()
                    .filter(name -> name.contains("parkour-") && getDifficulty(name) <= difficulty)
                    .toList()));

            schematicBlocks = rotatedPaste(schematic, selectBlocks().get(0).getLocation());

            particles(schematicBlocks);
            sound(schematicBlocks);

            new ParkourSchematicGenerateEvent(schematic, this, player).call();

            if (schematicBlocks.isEmpty()) {
                IP.logging().stack("Error while trying to paste schematic %s".formatted(schematic.getFile().getName()), new NoSuchElementException("No schematic blocks found"));
                return;
            }

            schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
            waitForSchematicCompletion = true;
            return;
        }

        List<Block> blocks = selectBlocks();

        if (blocks.isEmpty()) {
            IP.logging().stack("Error while trying to generate parkour", new NoSuchElementException("No blocks to generate found"));
            return;
        }

        List<Block> movedBlocks = new ArrayList<>();
        for (Block block : blocks) {
            BlockData data = (jump == JumpType.SPECIAL && !generatorOptions.contains(GeneratorOption.DISABLE_SPECIAL)) ? Probs.random(specialChances) : selectBlockData();

            if (data instanceof Fence) {
                block = block.getLocation().subtract(0, 1, 0).getBlock();
            }

            block.setBlockData(data, data instanceof Fence || data instanceof GlassPane);
            movedBlocks.add(block);
        }

        new ParkourBlockGenerateEvent(movedBlocks, this, player).call();

        particles(movedBlocks);
        sound(movedBlocks);

        history.addAll(movedBlocks);
        schematicCooldown--;
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

        /// snapped vector, supports no rotation if x == z
        Vector snapped;
        if (startToEnd.equals(heading)) {
            snapped = new Vector(0, 0, 0);
        } else if (Math.abs(startToEnd.getX()) > Math.abs(startToEnd.getZ())) {
            snapped = new Vector(Math.signum(startToEnd.getX()), 0, 0);
        } else {
            snapped = new Vector(0, 0, Math.signum(startToEnd.getZ()));
        }

        // the angle between heading and normalized direction of schematic, snapped to 90 deg angles
        double snappedAngle = angleInY(heading, snapped);

        Location rotatedStart = location.clone().subtract(start.clone().rotateAroundY(snappedAngle));
        Vector rotatedStartToEnd = startToEnd.clone().rotateAroundY(snappedAngle);

        history.add(location.clone().add(rotatedStartToEnd).subtract(0, 1, 0).getBlock());
        return schematic.paste(rotatedStart, snappedAngle); // only yaw
    }

    private double angleInY(Vector a, Vector b) {
        double det = a.getX() * b.getZ() - a.getZ() * b.getX();
        return Math.atan2(det, a.dot(b));
    }

    protected Block getLatest() {
        return history.get(history.size() - 1);
    }

    private double getDifficulty(String fileName) {
        String path = "difficulty.%s".formatted(fileName.split("[-.]")[1]);

        if (!Config.SCHEMATICS.isPath(path))  {
            return 1.0; // todo remove
        }

        return Config.SCHEMATICS.getDouble(path);
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
     * Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
     * the player to achieve this score using their settings.
     */
    public double getDifficultyScore() {
        double score = 0;

        if (profile.get("useSpecialBlocks").asBoolean()) score += 0.5;
        if (profile.get("schematicDifficulty").asDouble() > 0) {
            if (profile.get("schematicDifficulty").asDouble() <= 0.25) score += 0.2;
            else if (profile.get("schematicDifficulty").asDouble() <= 0.5) score += 0.3;
            else if (profile.get("schematicDifficulty").asDouble() <= 0.75) score += 0.4;
            else if (profile.get("schematicDifficulty").asDouble() <= 1.0) score += 0.5;
        }

        return score;
    }

    /**
     * @return The time in custom format.
     */
    public String getFormattedTime() {
        return getTime(Config.CONFIG.getString("options.time.score-format"));
    }

    /**
     * @return The current detailed duration of the run.
     */
    public String getDetailedTime() {
        return getTime("mm:ss:SSS");
    }

    private String getTime(String format) {
        var timeMs = Instant.now().minusMillis(start != null ? start.toEpochMilli() : Instant.now().toEpochMilli());

        try {
            return DateTimeFormatter.ofPattern(format)
                    .withZone(ZoneOffset.UTC)
                    .format(timeMs);
        } catch (IllegalArgumentException ex) {
            IP.logging().stack("Invalid score time format %s".formatted(Config.CONFIG.getString("options.time.score-format")), ex);
            return "";
        }
    }

    /**
     * @return This generator's mode.
     */
    public Mode getMode() {
        return Modes.DEFAULT;
    }

    /**
     * @return The players in this session.
     */
    @NotNull
    public List<ParkourPlayer> getPlayers() {
        return session.getPlayers();
    }

    /**
     * @return The spectators in this session.
     */
    @NotNull
    public List<ParkourSpectator> getSpectators() {
        return session.getSpectators();
    }

    public enum JumpType {
        DEFAULT, SCHEMATIC, SPECIAL
    }
}