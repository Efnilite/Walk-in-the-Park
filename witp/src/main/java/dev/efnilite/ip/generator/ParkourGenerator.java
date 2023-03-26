package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.api.Gamemode;
import dev.efnilite.ip.api.Gamemodes;
import dev.efnilite.ip.api.event.ParkourBlockGenerateEvent;
import dev.efnilite.ip.api.event.ParkourFallEvent;
import dev.efnilite.ip.api.event.ParkourScoreEvent;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.gamemode.DefaultGamemode;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.Score;
import dev.efnilite.ip.reward.RewardString;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.SchematicAdjuster;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.world.Island;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Numbers;
import dev.efnilite.vilib.util.Strings;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Slab;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link ParkourPlayer} has.
 *
 * @author Efnilite
 */
public class ParkourGenerator {

    /**
     * This generator's score
     */
    public int score = 0;

    /**
     * The zone in which the parkour can take place. (playable area)
     */
    public Location[] zone;

    /**
     * The direction of the parkour
     */
    public Vector heading = stringToVector(Option.HEADING);

    /**
     * Generator options
     */
    public List<GeneratorOption> generatorOptions;

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
     * The random for this Thread, which is useful in randomly generating parkour
     */
    protected final ThreadLocalRandom random = ThreadLocalRandom.current();

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
    public final HashMap<Integer, Integer> distanceChances = new HashMap<>();

    /**
     * Variable to determine how much the chance should be of a jump type, depending on the player's score
     */
    public final HashMap<Integer, Double> adaptiveDistanceChances = new HashMap<>();

    /**
     * The chances of which height the jump should have
     */
    public final HashMap<Integer, Integer> heightChances = new HashMap<>();

    /**
     * The chances of which type of special jump
     */
    public final HashMap<Integer, Integer> specialChances = new HashMap<>();

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    public final HashMap<Integer, Integer> defaultChances = new HashMap<>();

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
     * The amount of blocks that will trail the player's current index.
     */
    public int blockTrail = 2;

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

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param session The session associated with this generator
     */
    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = Arrays.asList(generatorOptions);

        player = session.getPlayers().get(0);
        island = new Island(session, Schematics.getSchematic("spawn-island.witp"));

        mostRecentBlock = player.getLocation().clone();
        lastStandingPlayerLocation = mostRecentBlock.clone();

        zone = WorldDivider.toSelection(session);

        calculateChances();
    }

    /**
     * Calculates all chances for every variable
     */
    public void calculateChances() {
        calculateAdaptiveDistance();
        calculateDefault();
        calculateHeight();
        calculateDistance();
        calculateSpecial();
    }

    /**
     * Calculates the chances of which type of special jump
     */
    public void calculateSpecial() {
        specialChances.clear();

        int percentage = 0;
        for (int i = 0; i < Option.SPECIAL_ICE; i++) {
            specialChances.put(percentage, 0);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_SLAB; i++) {
            specialChances.put(percentage, 1);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_PANE; i++) {
            specialChances.put(percentage, 2);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_FENCE; i++) {
            specialChances.put(percentage, 3);
            percentage++;
        }
    }

    /**
     * Calculates chances for adaptive distances
     */
    public void calculateAdaptiveDistance() {
        adaptiveDistanceChances.clear();

        double multiplier = Option.MULTIPLIER;
        adaptiveDistanceChances.put(1, (Option.MAXED_ONE_BLOCK - Option.NORMAL_ONE_BLOCK) / multiplier);
        adaptiveDistanceChances.put(2, (Option.MAXED_TWO_BLOCK - Option.NORMAL_TWO_BLOCK) / multiplier);
        adaptiveDistanceChances.put(3, (Option.MAXED_THREE_BLOCK - Option.NORMAL_THREE_BLOCK) / multiplier);
        adaptiveDistanceChances.put(4, (Option.MAXED_FOUR_BLOCK - Option.NORMAL_FOUR_BLOCK) / multiplier);
    }

    /**
     * Calculates the chances of default jump types
     */
    public void calculateDefault() {
        defaultChances.clear();

        int percentage = 0;
        for (int i = 0; i < Option.NORMAL; i++) { // normal
            defaultChances.put(percentage, 0);
            percentage++;
        }
        if (!generatorOptions.contains(GeneratorOption.DISABLE_SCHEMATICS)) { // schematics
            for (int i = 0; i < Option.SCHEMATICS; i++) {
                defaultChances.put(percentage, 1);
                percentage++;
            }
        }
        if (!generatorOptions.contains(GeneratorOption.DISABLE_SPECIAL)) { // special
            for (int i = 0; i < Option.SPECIAL; i++) {
                defaultChances.put(percentage, 2);
                percentage++;
            }
        }
    }

    /**
     * Calculates the chances of height
     */
    public void calculateHeight() {
        heightChances.clear();

        int percentage = 0;
        for (int i = 0; i < Option.NORMAL_UP; i++) {
            heightChances.put(percentage, 1);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_LEVEL; i++) {
            heightChances.put(percentage, 0);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_DOWN; i++) {
            heightChances.put(percentage, -1);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_DOWN2; i++) {
            heightChances.put(percentage, -2);
            percentage++;
        }
    }

    /**
     * Calculates the chances of distance, factoring in if the player uses adaptive difficulty
     */
    public void calculateDistance() {
        distanceChances.clear();

        // If the player uses difficulty, slowly increase the chances of harder jumps (depends on user settings though)
        int one, two, three, four;
        if (profile.get("useScoreDifficulty").asBoolean() && generatorOptions.contains(GeneratorOption.DISABLE_ADAPTIVE)) {

            if (score <= Option.MULTIPLIER) {
                one = (int) (Option.NORMAL_ONE_BLOCK + (adaptiveDistanceChances.get(1) * score));
                two = (int) (Option.NORMAL_TWO_BLOCK + (adaptiveDistanceChances.get(2) * score));
                three = (int) (Option.NORMAL_THREE_BLOCK + (adaptiveDistanceChances.get(3) * score));
                four = (int) (Option.NORMAL_FOUR_BLOCK + (adaptiveDistanceChances.get(4) * score));
            } else {
                one = Option.MAXED_ONE_BLOCK;
                two = Option.MAXED_TWO_BLOCK;
                three = Option.MAXED_THREE_BLOCK;
                four = Option.MAXED_FOUR_BLOCK;
            }
        } else {
            one = Option.NORMAL_ONE_BLOCK;
            two = Option.NORMAL_TWO_BLOCK;
            three = Option.NORMAL_THREE_BLOCK;
            four = Option.NORMAL_FOUR_BLOCK;
        }

        int percentage = 0;
        for (int i = 0; i < one; i++) { // regenerate the chances for distance
            distanceChances.put(percentage, 1);
            percentage++;
        }
        for (int i = 0; i < two; i++) {
            distanceChances.put(percentage, 2);
            percentage++;
        }
        for (int i = 0; i < three; i++) {
            distanceChances.put(percentage, 3);
            percentage++;
        }
        for (int i = 0; i < four; i++) {
            distanceChances.put(percentage, 4);
            percentage++;
        }
    }

    public Gamemode getGamemode() {
        return Gamemodes.DEFAULT;
    }

    public void updateScoreboard() {
        // board can be null a few ticks after on player leave
        if (player == null || player.board == null || player.board.isDeleted()) {
            return;
        }

        if (!(boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD)) {
            return;
        }

        if (!profile.get("showScoreboard").asBoolean()) {
            return;
        }

        Leaderboard leaderboard = getGamemode().getLeaderboard();

        String title = Strings.colour(Util.translate(player.player, Locales.getString(player.getLocale(), "scoreboard.title")));
        List<String> lines = new ArrayList<>();

        Score top = null, rank = null;
        if (leaderboard != null) {

            // only get score at rank if lines contains variables
            if (Util.listContains(lines, "topscore", "topplayer")) {
                top = leaderboard.getScoreAtRank(1);
            }

            rank = leaderboard.get(player.getUUID());
        }

        // set generic score if score is not found
        top = top == null ? new Score("?", "?", "?", 0) : top;
        rank = rank == null ? new Score("?", "?", "?", 0) : rank;

        // update lines
        for (String line : Colls.map(Strings::colour, Locales.getStringList(player.getLocale(), "scoreboard.lines"))) {
            line = Util.translate(player.player, line); // add support for PAPI placeholders in scoreboard

            lines.add(line
                    .replace("%score%", Integer.toString(score))
                    .replace("%time%", getTime())
                    .replace("%highscore%", Integer.toString(rank.score()))
                    .replace("%highscoretime%", rank.time())
                    .replace("%topscore%", Integer.toString(top.score()))
                    .replace("%topplayer%", top.name())
                    .replace("%session%", session.getPlayers().get(0).getName())); // todo find a better way
        }

        player.board.updateTitle(title
                .replace("%score%", Integer.toString(score))
                .replace("%time%", getTime())
                .replace("%highscore%", Integer.toString(rank.score()))
                .replace("%highscoretime%", rank.time())
                .replace("%topscore%", Integer.toString(top.score()))
                .replace("%topplayer%", top.name())
                .replace("%session%", session.getPlayers().get(0).getName()));
        player.board.updateLines(lines);
    }

    public void updatePreferences() {
        // no preferences here...
    }

    public void particles(List<Block> applyTo) {
        if (profile.get("particles").asBoolean()) {
            ParticleData<?> data = Option.PARTICLE_DATA;

            // display particle
            switch (Option.PARTICLE_SHAPE) {
                case DOT -> {
                    data.speed(0.4).size(20).offsetX(0.5).offsetY(1).offsetZ(0.5);
                    Particles.draw(mostRecentBlock.clone().add(0.5, 1, 0.5), data);
                }
                case CIRCLE -> {
                    data.size(5);
                    Particles.circle(mostRecentBlock.clone().add(0.5, 0.5, 0.5), data, (int) Math.sqrt(applyTo.size()), 20);
                }
                case BOX -> {
                    Location min = new Location(blockSpawn.getWorld(), Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    Location max = new Location(blockSpawn.getWorld(), Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
                    for (Block block : applyTo) {
                        Location loc = block.getLocation();
                        min = Locations.min(min, loc);
                        max = Locations.max(max, loc);
                    }

                    if (min.getBlockX() == Integer.MIN_VALUE || max.getBlockX() == Integer.MAX_VALUE) { // to not crash the server (lol)
                        return;
                    }

                    data.size(1);
                    Particles.box(BoundingBox.of(max, min), player.player.getWorld(), data, 0.2);
                }
            }

        }

        if (profile.get("sound").asBoolean()) {
            // play sound
            for (ParkourPlayer viewer : session.getPlayers()) {
                viewer.player.playSound(mostRecentBlock, Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
            }
            for (ParkourSpectator viewer : session.getSpectators()) {
                viewer.player.playSound(mostRecentBlock, Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
            }
        }
    }

    public BlockData selectBlockData() {
        String style = profile.get("style").value();

        Material material = IP.getRegistry().getTypeFromStyle(style).get(style);

        // if found style is null, get the first registered style to prevent big boy errors
        if (material == null) {
            String newStyle = new ArrayList<>(IP.getRegistry().getStyleTypes().get(0).getStyles().keySet()).get(0);

            profile.set("style", newStyle);

            return selectBlockData();
        } else {
            return material.createBlockData();
        }
    }

    public List<Block> selectBlocks() {
        int dy = getRandomChance(heightChances);
        int gap = getRandomChance(distanceChances);

        if (dy > 0 && gap < 2) { // prevent blocks from spawning on top of each other
            gap = 2;
        }

        return List.of(selectNext(mostRecentBlock, gap, dy));
    }

    /**
     * Selects the next block that will continue the parkour.
     * This is done by choosing a random value for the sideways movement.
     * Based on this sideways movement, a value for forward movement will be chosen.
     * This is done to ensure players are able to complete the jump.
     *
     * @param current The current location to check from.
     * @param range   The range that should be checked.
     * @param dy      The difference in height.
     * @return a randomly selected block.
     */
    protected Block selectNext(Location current, int range, int dy) {
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

        switch (mostRecentBlock.getBlock().getType()) {
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
            ds = random.nextInt(-adjustedRange + 1, adjustedRange);
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
        Location clone = current.clone();

        // add all offsets to a vector and rotate it to match current direction
        offset.rotateAroundY(stringToVector(Option.HEADING).angle(heading));

        clone.add(offset);

        return clone.getBlock();
    }

    private Vector stringToVector(String direction) {
        return switch (direction.toLowerCase()) {
            case "north" -> new Vector(0, 0, -1);
            case "south" -> new Vector(0, 0, 1);
            case "west" -> new Vector(-1, 0, 0);
            default -> new Vector(1, 0, 0); // east
        };
    }

    /**
     * Calculates the player's position in a parameter form, to make it easier to detect when the player is near the edge of the border.
     * Returns a 2-dimensional array where the first array index is used to select the x, y and z (0, 1 and 2 respectively).
     * This returns an array where the first index is tx and second index is borderMarginX (see comments below for explanation).
     *
     * @return a 2-dimensional array where the first array index is used to specify x, y and z and the second used to specify the type.
     */
    public double[][] calculateParameterization() {
        Location min = zone[0];
        Location max = zone[1];

        // the total dimensions
        double dx = max.getX() - min.getX();
        double dy = max.getY() - min.getY();
        double dz = max.getZ() - min.getZ();

        // the relative x, y and z coordinates
        // relative being from the min point of the selection zone
        double relativeX = mostRecentBlock.getX() - min.getX();
        double relativeY = mostRecentBlock.getY() - min.getY();
        double relativeZ = mostRecentBlock.getZ() - min.getZ();

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

    /**
     * Updates the heading to make sure it avoids the border of the selected zone.
     * When the most recent block is detected to be within a 5-block radius of the border,
     * the heading will automatically be turned around to ensure that the edge does not get
     * destroyed.
     *
     * @param progress The 2-dimensional array resulting from {@link #calculateParameterization()}
     * @return a list of new proposed headings
     */
    public List<Vector> updateHeading(double[][] progress) {
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
            // x should Vector
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

    /**
     * Updates the height to make sure the player doesn't go below the playable zone.
     * If the current height is fine, it will return the value of parameter currentHeight.
     * If the current height is within the border margin, it will return a value (1 or -1)
     * to make sure the player doesn't go below this value
     *
     * @param progress      The 2-dimensional array generated by {@link #calculateParameterization()}
     * @param currentHeight The height the system wants to currently use
     * @return the updated height
     */
    public int updateHeight(double[][] progress, int currentHeight) {
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

    public void score() {
        score++;
        totalScore++;
        checkRewards();

        new ParkourScoreEvent(player).call();
    }

    public void fall() {
        new ParkourFallEvent(player).call();
        reset(true);
    }

    public void menu() {
        Menus.PARKOUR_SETTINGS.open(player);
    }

    public void startTick() {
        task = Task.create(IP.getPlugin())
                .repeat(generatorOptions.contains(GeneratorOption.INCREASED_TICK_ACCURACY) ? 1 : Option.GENERATOR_CHECK)
                .execute(new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    return;
                }

                tick();
            }
        }).run();
    }

    /**
     * Starts the check
     */
    public void tick() {
        if (session == null) {
            reset(false);
            return;
        }

        session.getPlayers().forEach(player -> player.updateVisualTime(player.selectedTime));
        updateScoreboard();

        session.getSpectators().forEach(ParkourSpectator::update);
        player.player.setSaturation(20);

        Location playerLocation = player.getLocation();

        if (playerLocation.getWorld() != playerSpawn.getWorld()) { // sometimes player worlds don't match (somehow)
            player.teleport(playerSpawn);
            return;
        }

        if (lastStandingPlayerLocation.getY() - playerLocation.getY() > 10 && playerSpawn.distance(playerLocation) > 5) { // Fall check
            fall();
            return;
        }

        Location belowPlayer = playerLocation.clone().subtract(0, 1, 0);
        Block blockBelowPlayer = belowPlayer.getBlock(); // Get the block below

        if (blockBelowPlayer.getType() == Material.AIR) {
            if (belowPlayer.subtract(0, 0.5, 0).getBlock().getType() == Material.AIR) {
                return;
            }
            blockBelowPlayer = belowPlayer.getBlock();
        }

        if (schematicBlocks.contains(blockBelowPlayer) && blockBelowPlayer.getType() == Material.RED_WOOL && !deleteStructure) { // Structure deletion check
            for (int i = 0; i < 10; i++) {
                score();
            }
            waitForSchematicCompletion = false;
            schematicCooldown = Option.SCHEMATIC_COOLDOWN;
            generate(profile.get("blockLead").asInt());
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

        if (start == null) { // start stopwatch when first point is achieved
            start = Instant.now();
        }

        lastStandingPlayerLocation = playerLocation.clone();

        int blockLead = profile.get("blockLead").asInt();

        int deltaCurrentTotal = positionIndexTotal - currentIndex; // delta between current index and total
        if (deltaCurrentTotal <= blockLead) {
            generate(blockLead - deltaCurrentTotal + 1); // generate the remaining amount so it will match
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

        if (Option.ALL_POINTS) { // score handling
            for (int i = 0; i < deltaFromLast; i++) { // score the difference
                score();
            }
        } else {
            score();
        }

        calculateDistance();
    }

    /**
     * Resets the parkour
     *
     * @param regenerate false if this is the last reset (when the player leaves), true for resets by falling
     */
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
        deleteStructure();

        if (regenerate) {
            player.teleport(playerSpawn);
        }

        Leaderboard leaderboard = getGamemode().getLeaderboard();

        Score record = null;
        if (leaderboard != null) {
            record = leaderboard.get(player.getUUID());
        }

        if (record == null) {
            record = new Score(player.getName(), "?", "?", 0);
        }

        int score = this.score;
        String time = getTime();

        if (profile.get("showFallMessage").asBoolean() && regenerate) {
            String message;
            int number = 0;
            if (score == record.score()) {
                message = "settings.parkour_settings.items.fall_message.formats.tied";
            } else if (score > record.score()) {
                number = score - record.score();
                message = "settings.parkour_settings.items.fall_message.formats.beat";
            } else {
                number = record.score() - score;
                message = "settings.parkour_settings.items.fall_message.formats.miss";
            }

            for (ParkourPlayer players : session.getPlayers()) {
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
                players.sendTranslated("settings.parkour_settings.items.fall_message.score", Integer.toString(score));
                players.sendTranslated("settings.parkour_settings.items.fall_message.time", time);
                players.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record.score()));
                players.sendTranslated(message, Integer.toString(number));
                players.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            }
        }

        if (leaderboard != null && score > record.score()) {
            registerScore();
        }

        this.score = 0;
        start = null;

        if (regenerate) { // generate back the blocks
            generateFirst(playerSpawn, blockSpawn);
        } else {
            island.destroy();
        }
    }

    /**
     * @return The current duration of the run.
     */
    public String getTime() {
        if (start == null) {
            return "0.0s";
        }

        Duration duration = Duration.between(start, Instant.now());

        StringJoiner builder = new StringJoiner(" ");

        if (duration.toHoursPart() > 0) {
            builder.add("%dh".formatted(duration.toHoursPart()));
        }
        if (duration.toMinutesPart() > 0) {
            builder.add("%dm".formatted(duration.toMinutesPart()));
        }
        if (duration.toSecondsPart() > 0 || duration.toMillisPart() > 0) {
            long rounded = Math.round(duration.toMillisPart() / 100.0);

            if (rounded > 9) rounded = 9;

            builder.add("%d.%ss".formatted(duration.toSecondsPart(), rounded));
        }

        return builder.toString();
    }

    protected void registerScore() {
        getGamemode().getLeaderboard().put(player.getUUID(), new Score(player.getName(), getTime(), player.calculateDifficultyScore(), score));
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

        int type = getRandomChance(defaultChances); // 0 = normal, 1 = structures, 2 = special

        if (type == 1) {
            type = schematicCooldown == 0 && profile.get("useSchematic").asBoolean() ? type : Numbers.random(0, 2) * 2;
        }

        switch (type) {
            case 0, 2 -> {
                BlockData next;

                if (type == 2 && profile.get("useSpecialBlocks").asBoolean()) { // if special
                    int value = getRandomChance(specialChances);
                    switch (value) {
                        // ice
                        case 0 -> next = Material.PACKED_ICE.createBlockData();
                        // slab
                        case 1 -> {
                            next = Material.SMOOTH_QUARTZ_SLAB.createBlockData();
                            ((Slab) next).setType(Slab.Type.BOTTOM);
                        }
                        // pane
                        case 2 -> next = Material.WHITE_STAINED_GLASS_PANE.createBlockData();
                        // fence
                        case 3 -> next = Material.OAK_FENCE.createBlockData();
                        // ???
                        default -> {
                            next = Material.STONE.createBlockData();
                            IP.logging().stack("Invalid special block ID " + value, new IllegalArgumentException());
                        }
                    }
                } else {
                    next = selectBlockData();
                }

                List<Block> blocks = selectBlocks();
                if (blocks.isEmpty()) {
                    return;
                }

                Block selectedBlock = blocks.get(0);

                if (next.getMaterial() == Material.OAK_FENCE) {
                    selectedBlock = WorldManager.getWorld().getBlockAt(selectedBlock.getX(), mostRecentBlock.getBlockY(), selectedBlock.getZ());
                }

                setBlock(selectedBlock, next);
                new ParkourBlockGenerateEvent(selectedBlock, this, player).call();

                positionIndexMap.put(selectedBlock, positionIndexTotal);
                positionIndexTotal++;

                mostRecentBlock = selectedBlock.getLocation().clone();

                particles(blocks);

                if (schematicCooldown > 0) {
                    schematicCooldown--;
                }
            }
            case 1 -> {
                File folder = IP.getInFolder("schematics");
                List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.contains("parkour-")));
                File file = null;
                if (!files.isEmpty()) {
                    boolean passed = true;
                    while (passed) {
                        file = files.get(random.nextInt(files.size()));
                        if (profile.get("schematicDifficulty").asDouble() == 0) {
                            profile.set("schematicDifficulty", "0.2");
                        }
                        if (getDifficulty(file.getName()) < profile.get("schematicDifficulty").asDouble()) {
                            passed = false;
                        }
                    }
                } else {
                    IP.logging().error("No structures to choose from!");
                    generate(); // generate if no schematic is found
                    return;
                }
                Schematic schematic = Schematics.getSchematic(file.getName());

                schematicCooldown = Option.SCHEMATIC_COOLDOWN;
                List<Block> blocks = selectBlocks();
                if (blocks.isEmpty()) {
                    return;
                }

                Block selectedBlock = blocks.get(0);

                try {
                    schematicBlocks = SchematicAdjuster.pasteAdjusted(schematic, selectedBlock.getLocation());
                    waitForSchematicCompletion = true;
                } catch (IOException ex) {
                    IP.logging().stack("There was an error while trying to paste schematic " + schematic.getName(), "delete this file and restart the server", ex);
                    reset(true);
                    return;
                }

                if (schematicBlocks == null || schematicBlocks.isEmpty()) {
                    IP.logging().error("0 blocks found in structure!");
                    player.send("<red>There was an error while trying to paste a structure! If you don't want this to happen again, you can disable them in the menu.");
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
            default -> IP.logging().stack("Illegal jump type with id " + type, new IllegalArgumentException());
        }
    }

    /**
     * Gets the difficulty of a schematic according to schematics.yml
     *
     * @param fileName The name of the file (parkour-x.nbt)
     * @return the difficulty, ranging from 0 to 1
     */
    private double getDifficulty(String fileName) {
        int index = Integer.parseInt(fileName.split("-")[1].replace(".witp", ""));

        return Config.SCHEMATICS.getDouble("difficulty.%d".formatted(index));
    }

    /**
     * Generates a specific amount of blocks ahead of the player
     *
     * @param amount The amount
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

        return map.get(Colls.random(keys));
    }

    /**
     * Checks a player's rewards and gives them if necessary
     */
    public void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || score == 0 || totalScore == 0) {
            return;
        }

        if (!(getGamemode() instanceof DefaultGamemode)) {
            return;
        }

        // check generic score rewards
        List<RewardString> strings = Rewards.SCORE_REWARDS.get(score);
        if (strings != null) {
            strings.forEach(s -> s.execute(player));
        }

        // gets the correct type of score to check based on the config option
        int typeToCheck = Option.REWARDS_USE_TOTAL_SCORE ? totalScore : score;
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (typeToCheck % interval == 0) {
                strings = Rewards.INTERVAL_REWARDS.get(interval);
                strings.forEach(s -> s.execute(player));
            }
        }

        strings = Rewards.ONE_TIME_REWARDS.get(score);
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
        schematicCooldown = Option.SCHEMATIC_COOLDOWN;
    }

    protected void setBlock(Block block, BlockData data) {
        if (data instanceof Fence || data instanceof GlassPane) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data, false);
        }

        // fixes players receiving delayed update
        player.player.sendBlockChange(block.getLocation(), data);
    }

    /**
     * Generates the first few blocks (which come off the spawn island)
     *
     * @param spawn The spawn of the player
     * @param block The location used to begin the parkour of off
     */
    public void generateFirst(Location spawn, Location block) {
        playerSpawn = spawn.clone();
        lastStandingPlayerLocation = spawn.clone();
        blockSpawn = block.clone();
        mostRecentBlock = block.clone();

        generate(profile.get("blockLead").asInt() + 1);
    }
}