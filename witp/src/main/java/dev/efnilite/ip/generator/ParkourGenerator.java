package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Mode;
import dev.efnilite.ip.api.Modes;
import dev.efnilite.ip.api.event.ParkourBlockGenerateEvent;
import dev.efnilite.ip.api.event.ParkourFallEvent;
import dev.efnilite.ip.api.event.ParkourScoreEvent;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.gamemode.DefaultGamemode;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.reward.RewardString;
import dev.efnilite.ip.reward.Rewards;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.schematic.Schematics;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Probs;
import dev.efnilite.ip.world.WorldDivider;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Numbers;
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
     * The amount of blocks that trail behind the player.
     */
    private static int BLOCK_TRAIL = 2;

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
    public final HashMap<Material, Double> specialChances = new HashMap<>();

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    public final HashMap<Integer, Double> defaultChances = new HashMap<>();

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
    protected boolean deleteStructure = false;
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

    public List<Block> blocks = new ArrayList<>();

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

        defaultChances.put(0, Option.NORMAL);
        defaultChances.put(1, Option.SCHEMATICS);
        defaultChances.put(2, Option.SPECIAL);

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

        specialChances.put(Material.PACKED_ICE, Option.SPECIAL_ICE);
        specialChances.put(Material.SMOOTH_QUARTZ_SLAB, Option.SPECIAL_SLAB);
        specialChances.put(Material.GLASS_PANE, Option.SPECIAL_PANE);
        specialChances.put(Material.OAK_FENCE, Option.SPECIAL_FENCE);
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

    protected void sound() {
        if (!profile.get("sound").asBoolean()) {
            return;
        }

        // play sound
        for (ParkourPlayer viewer : session.getPlayers()) {
            viewer.player.playSound(getLatest().getLocation(), Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
        }
        for (ParkourSpectator viewer : session.getSpectators()) {
            viewer.player.playSound(getLatest().getLocation(), Option.SOUND_TYPE, 4, Option.SOUND_PITCH);
        }
    }

    protected BlockData selectBlockData() {
        String style = profile.get("style").value();

        Material material = IP.getRegistry().getTypeFromStyle(style).get(style);

        // if found style is null, get the first registered style to prevent big boy errors
        if (material == null) {
            String newStyle = new ArrayList<>(IP.getRegistry().getStyleTypes().get(0).styles.keySet()).get(0);

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
        if (!Rewards.REWARDS_ENABLED || score == 0 || totalScore == 0) {
            return;
        }
        if (!(getMode() instanceof DefaultGamemode)) {
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
                .execute(new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    this.cancel();
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

        for (ParkourPlayer other : session.getPlayers()) {
            updateVisualTime(other, other.selectedTime);
            other.updateScoreboard(this);
            other.player.setSaturation(20);
        }
        for (ParkourSpectator spectator : session.getSpectators()) {
            spectator.update();
        }

        if (player.getLocation().subtract(lastStandingPlayerLocation).getY() < -10) { // Fall check
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

        if (schematicBlocks.contains(blockBelowPlayer) && blockBelowPlayer.getType() == Material.RED_WOOL && !deleteStructure) { // Structure deletion check
            for (int i = 0; i < calculateDifficultyScore() * 15; i++) {
                score();
            }
            waitForSchematicCompletion = false;
            schematicCooldown = Option.SCHEMATIC_COOLDOWN;
            generate(profile.get("blockLead").asInt());
            deleteStructure = true;
            return;
        }

        if (!blocks.contains(blockBelowPlayer)) {
            return; // player is on an unknown block
        }

        int currentIndex = blocks.indexOf(blockBelowPlayer); // current index of the player
        int deltaFromLast = currentIndex - lastPositionIndexPlayer;

        if (deltaFromLast <= 0) { // the player is actually making progress and not going backwards (current index is higher than the previous)
            return;
        }

        lastStandingPlayerLocation = player.getLocation();

        int blockLead = profile.get("blockLead").asInt();

        int deltaCurrentTotal = blocks.size() - currentIndex; // delta between current index and total
        if (deltaCurrentTotal <= blockLead) {
            generate(blockLead - deltaCurrentTotal); // generate the remaining amount so it will match
        }
        lastPositionIndexPlayer = currentIndex;

        // delete trailing blocks
        for (int idx = 0; idx < blocks.size(); idx++) {
            Block block = blocks.get(idx);

            if (currentIndex - idx > BLOCK_TRAIL) {
                block.setType(Material.AIR);
            }
        }

        if (deleteStructure) { // deletes the structure if the player goes to the next block (reason why it's last)
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
     * Resets the parkour
     *
     * @param regenerate false if this is the last reset (when the player leaves), true for resets by falling
     */
    public void reset(boolean regenerate) {
        stopped = !regenerate;

        if (!regenerate && task == null) {
            IP.logging().warn("## Incomplete joining setup.");
            IP.logging().warn("## There has probably been an error somewhere. Please report this error!");
            IP.logging().warn("## You don't have to report this warning.");
        }

        lastPositionIndexPlayer = 0;
        blocks.forEach(block -> block.setType(Material.AIR));
        blocks.clear();

        waitForSchematicCompletion = false;
        deleteStructure();

        if (regenerate) {
            player.teleport(playerSpawn);
        }

        Leaderboard leaderboard = getMode().getLeaderboard();

        Score record = null;
        if (leaderboard != null) {
            record = leaderboard.get(player.getUUID());
        }

        if (record == null) {
            record = new Score(player.getName(), "?", "?", 0);
        }

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
            leaderboard.put(player.getUUID(), new Score(player.getName(), getTime(), Double.toString(calculateDifficultyScore()).substring(0, 3), score));
        }

        score = 0;
        start = null;

        if (regenerate) { // generate back the blocks
            generateFirst(playerSpawn, blockSpawn);
            return;
        }

        island.destroy();
    }

    // Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
    // the player to achieve this score using their settings.
    private double calculateDifficultyScore() {
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

    private void deleteStructure() {
        schematicBlocks.forEach(block -> block.setType(Material.AIR));
        schematicBlocks.clear();

        deleteStructure = false;
        schematicCooldown = Option.SCHEMATIC_COOLDOWN;
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
        playerSpawn = spawn.clone();
        lastStandingPlayerLocation = spawn.clone();
        blockSpawn = block.clone();

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

        int type = Probs.random(defaultChances); // 0 = normal, 1 = structures, 2 = special

        if (type == 1) {
            type = schematicCooldown == 0 && profile.get("useSchematic").asBoolean() ? type : Numbers.random(0, 2) * 2;
        }

        switch (type) {
            case 0, 2 -> {
                BlockData next;

                if (type == 2 && profile.get("useSpecialBlocks").asBoolean()) { // if special
                    next = Probs.random(specialChances).createBlockData();

                    if (next instanceof Slab) {
                        ((Slab) next).setType(Slab.Type.BOTTOM);
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
                    selectedBlock = WorldManager.getWorld().getBlockAt(selectedBlock.getX(), getLatest().getY(), selectedBlock.getZ());
                }

                if (next instanceof Fence || next instanceof GlassPane) {
                    selectedBlock.setType(next.getMaterial(), true);
                } else {
                    selectedBlock.setBlockData(next, false);
                }

                new ParkourBlockGenerateEvent(selectedBlock, this, player).call();

                blocks.add(selectedBlock);

                particles(blocks);
                sound();

                if (schematicCooldown > 0) {
                    schematicCooldown--;
                }
            }
            case 1 -> {
                List<String> schematics = new ArrayList<>(Schematics.CACHE.keySet());

                // select random schematic
                double difficulty = profile.get("schematicDifficulty").asDouble();
                String schematicName;
                while (getDifficulty(schematicName = Colls.random(schematics)) <= difficulty) {}

                Schematic schematic = Schematics.CACHE.get(schematicName);

                schematicCooldown = Option.SCHEMATIC_COOLDOWN;
                schematicBlocks = rotatedPaste(schematic, selectBlocks().get(0).getLocation());

                if (schematicBlocks.isEmpty()) {
                    IP.logging().stack("Error while trying to paste schematic %s".formatted(schematic.getFile().getName()), new NullPointerException());
                    player.send("<red><bold>Error while trying to paste schematic. Contact the server owner.");
                    generate();
                    return;
                }

                waitForSchematicCompletion = true;
            }
            default -> IP.logging().stack("Error while trying to generate parkour with id %d".formatted(type), new IllegalArgumentException());
        }
    }

    private @NotNull List<Block> rotatedPaste(Schematic schematic, Location location) {
        if (schematic == null || location == null) {
            return Collections.emptyList();
        }

        Optional<Vector> optionalStart = schematic.getVectorBlockMap().entrySet().stream()
                .filter(e -> e.getValue().getMaterial() == Material.GREEN_WOOL)
                .map(Map.Entry::getKey)
                .findAny();

        Optional<Vector> optionalEnd = schematic.getVectorBlockMap().entrySet().stream()
                .filter(e -> e.getValue().getMaterial() == Material.RED_WOOL)
                .map(Map.Entry::getKey)
                .findAny();

        if (optionalStart.isEmpty()) {
            IP.logging().stack("Error while trying to find start of schematic",
                    "check if you placed a green wool block");
            return Collections.emptyList();
        }
        if (optionalEnd.isEmpty()) {
            IP.logging().stack("Error while trying to find end of schematic",
                    "check if you placed a red wool block");
            return Collections.emptyList();
        }

        Vector start = optionalStart.get();
        Vector end = optionalEnd.get();

        // update most recent block
        blocks.add(location.clone().add(end).getBlock());

        // use the approximate direction of the schematic to determine if
        // and by how much we need to rotate the schematic to line up to the current heading.
        Vector direction = end.clone()
                .subtract(start)
                .setY(0) // avoid pitching
                .normalize();

        double angle = direction.angle(heading);
        Vector angledDirection = direction.clone().rotateAroundY(angle);

        return schematic.paste(location.subtract(start), angledDirection);
    }

    private Block getLatest() {
        return blocks.get(blocks.size() - 1);
    }

    private double getDifficulty(String fileName) {
        return Config.SCHEMATICS.getDouble("difficulty.%d".formatted(Integer.parseInt(fileName.split("[-.]")[1])));
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

    /**
     * @return This generator's mode.
     */
    public Mode getMode() {
        return Modes.DEFAULT;
    }
}