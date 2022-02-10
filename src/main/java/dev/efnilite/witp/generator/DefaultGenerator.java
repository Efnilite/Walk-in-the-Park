package dev.efnilite.witp.generator;

import dev.efnilite.fycore.particle.ParticleData;
import dev.efnilite.fycore.particle.Particles;
import dev.efnilite.fycore.util.Logging;
import dev.efnilite.fycore.util.Task;
import dev.efnilite.fycore.util.Version;
import dev.efnilite.witp.ParkourMenu;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.BlockGenerateEvent;
import dev.efnilite.witp.events.PlayerFallEvent;
import dev.efnilite.witp.events.PlayerScoreEvent;
import dev.efnilite.witp.generator.base.DefaultGeneratorBase;
import dev.efnilite.witp.generator.base.GeneratorOption;
import dev.efnilite.witp.generator.subarea.Direction;
import dev.efnilite.witp.generator.subarea.SubareaDivider;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.SchematicAdjuster;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Wall;
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
 * {@link SubareaDivider}.
 *
 * @author Efnilite
 */
public class DefaultGenerator extends DefaultGeneratorBase {

    private BukkitRunnable task;

    private boolean isSpecial;
    private Material specialType;

    protected int totalScore;
    protected int schematicCooldown;
    protected boolean deleteStructure;
    protected boolean stopped;
    protected boolean waitForSchematicCompletion;

    /**
     * The most recently spawned block
     */
    protected Location mostRecentBlock;

    /**
     * The last location the player was found standing in
     */
    protected Location lastStandingPlayerLocation;

    /**
     * Where the player spawns on reset
     */
    protected Location playerSpawn;

    /**
     * Where blocks from schematics spawn
     */
    protected Location blockSpawn;

    /**
     * A list of blocks from the (possibly) spawned structure
     */
    protected List<Block> schematicBlocks;

    /**
     * The count total. Always bigger (or the same) than the positionIndexPlayer
     */
    protected int positionIndexTotal;

    /**
     * The player's current position index.
     */
    protected int lastPositionIndexPlayer;

    /**
     * A map which stores all blocks and their number values. The first block generated will have a value of 0.
     */
    protected final LinkedHashMap<Block, Integer> positionIndexMap;

    protected static final ParticleData<?> PARTICLE_DATA = new ParticleData<>(Particle.SPELL_INSTANT, null, 10, 0, 0, 0, 0);

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param player The player associated with this generator
     */
    public DefaultGenerator(@NotNull ParkourPlayer player, GeneratorOption... generatorOptions) {
        super(player, generatorOptions);
        Logging.verbose("Init of DefaultGenerator of " + player.getPlayer().getName());

        this.score = 0;
        this.totalScore = 0;
        this.stopped = false;
        this.waitForSchematicCompletion = false;
        this.schematicCooldown = 20;
        this.mostRecentBlock = player.getLocation().clone();
        this.lastStandingPlayerLocation = mostRecentBlock.clone();
        this.schematicBlocks = new ArrayList<>();
        this.deleteStructure = false;

        this.positionIndexTotal = 0;
        this.lastPositionIndexPlayer = -1;
        this.positionIndexMap = new LinkedHashMap<>();

        this.heading = Option.HEADING.get();
    }

    @Override
    public void particles(List<Block> applyTo) {
        if (player.useParticlesAndSound && Version.isHigherOrEqual(Version.V1_9)) {
            PARTICLE_DATA.type(Option.PARTICLE_TYPE.get());

            switch (Option.ParticleShape.valueOf(Option.PARTICLE_SHAPE.get().toUpperCase())) {
                case DOT:
                    PARTICLE_DATA.speed(0.4).size(20).offsetX(0.5).offsetY(1).offsetZ(0.5);
                    Particles.draw(mostRecentBlock.clone().add(0.5, 1, 0.5), PARTICLE_DATA, player.getPlayer());
                    break;
                case CIRCLE:
                    PARTICLE_DATA.size(5);
                    Particles.circle(mostRecentBlock.clone().add(0.5, 0.5, 0.5), PARTICLE_DATA, player.getPlayer(), (int) Math.sqrt(applyTo.size()), 25);
                    break;
                case BOX:
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
                    break;
            }
            player.getPlayer().playSound(mostRecentBlock.clone(), Option.SOUND_TYPE.get(), 4, Option.SOUND_PITCH.get());
        }
    }

    @Override
    public BlockData selectBlockData() {
        return player.randomMaterial().createBlockData();
    }

    @Override
    public List<Block> selectBlocks() {
        int height;
        int gap = getRandomChance(distanceChances) + 1;

        int deltaYMax = Option.MAX_Y.get() - mostRecentBlock.getBlockY();
        int deltaYMin = mostRecentBlock.getBlockY() - Option.MIN_Y.get();

        if (deltaYMax < 0) {
            height = -1;
        } else if (deltaYMin < 0) {
            height = 1;
        } else {
            height = getRandomChance(heightChances);
        }

        if (isSpecial && specialType != null) {
            switch (specialType) { // adjust for special jumps
                case PACKED_ICE: // ice
                    gap++;
                    break;
                case QUARTZ_SLAB: // slab
                    height = Math.min(height, 0);
                    break;
                case GLASS_PANE: // pane
                    gap -= 0.5;
                    break;
                case OAK_FENCE:
                    height = Math.min(height, 0);
                    gap -= 1;
                    break;
            }
        }

        if (mostRecentBlock.getBlock().getType() == Material.QUARTZ_SLAB) { // slabs cant go higher than one
            height = Math.min(height, 0);
        }

        height = Math.min(height, 1);
        gap = Math.min(gap, 4);

        List<Block> possible = getPossiblePositions(gap - height, height);

        if (possible.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(possible.get(random.nextInt(possible.size())));
    }

    @Override
    public void score() {
        score++;
        totalScore++;
        checkRewards();
    }

    @Override
    public void fall() {
        new PlayerFallEvent(player).call();
        reset(true);
    }

    @Override
    public void menu() {
        ParkourMenu.openMainMenu(player);
    }

    @Override
    public void startTick() {
        Logging.verbose("Starting generator of " + player.getPlayer().getName());

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
        new Task()
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
        updateSpectators();
        player.updateScoreboard();
        player.getPlayer().setSaturation(20);

        Location playerLocation = player.getLocation();

        if (playerLocation.getWorld() != playerSpawn.getWorld()) { // sometimes player worlds dont match (somehow)
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

        new PlayerScoreEvent(player).call();
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
            if (currentIndex - index > 2) {
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
                Logging.warn("Incomplete joining setup: there has probably been an error somewhere. Please report this error to the developer!");
                Logging.warn("You don't have to report this warning.");
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
            if (isNearBorder(mostRecentBlock.clone().toVector()) && score > 0) {
                heading = heading.turnRight(); // reverse heading if close to border
            }

            BlockData selectedBlockData = selectBlockData();

            if (isSpecial && player.useSpecialBlocks) { // if special
                switch (getRandomChance(specialChances)) {
                    case 0: // ice
                        selectedBlockData = Material.PACKED_ICE.createBlockData();
                        break;
                    case 1: // slab
                        selectedBlockData = Material.QUARTZ_SLAB.createBlockData();
                        ((Slab) selectedBlockData).setType(Slab.Type.BOTTOM);
                        break;
                    case 2: // pane
                        selectedBlockData = Material.WHITE_STAINED_GLASS_PANE.createBlockData();
                        break;
                    case 3: // fence
                        selectedBlockData = Material.OAK_FENCE.createBlockData();
                        break;
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

            particles(Collections.singletonList(selectedBlock));

            if (schematicCooldown > 0) {
                schematicCooldown--;
            }
        } else if (type == 1) {
            if (isNearBorder(mostRecentBlock.clone().toVector()) && score > 0) {
                generate(); // generate a normal block
                return;
            }

            File folder = new File(WITP.getInstance().getDataFolder() + "/schematics/");
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
                Logging.error("No structures to choose from!");
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
                Logging.stack("There was an error while trying to paste schematic " + schematic.getName(),
                        "This file might have been manually edited - please report this error to the developer!", ex);
                reset(true);
                return;
            }

            if (schematicBlocks == null || schematicBlocks.isEmpty()) {
                Logging.error("0 blocks found in structure!");
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

    protected int getRandomChance(HashMap<Integer, Integer> map) {
        List<Integer> keys = new ArrayList<>(map.keySet());
        if (keys.isEmpty()) {
            calculateChances();
            return 1;
        }
        int index = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        return map.get(index);
    }

    private void checkRewards() {
        if (!Option.REWARDS.get()) {
            return;
        }

        // Rewards
        HashMap<Integer, List<String>> scores = Option.REWARDS_SCORES;
        if (!scores.isEmpty() && scores.containsKey(score) && scores.get(score) != null) {
            List<String> commands = scores.get(score);
            if (commands != null) {
                if (Option.LEAVE_REWARDS.get()) {
                    rewardsLeaveList.addAll(commands);
                } else {
                    for (String command : commands) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player.getPlayer().getName()));
                    }
                }
            }
        }

        // Interval rewards
        if (Option.REWARDS_INTERVAL.get() > 0 && totalScore % Option.REWARDS_INTERVAL.get() == 0) {
            if (Option.INTERVAL_REWARDS_SCORES != null) {
                for (String command : Option.INTERVAL_REWARDS_SCORES) {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            command.replace("%player%", player.getPlayer().getName()));
                }
            }
            if (Option.REWARDS_COMMANDS.get() != null) {
                for (String command : Option.REWARDS_COMMANDS.get()) {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player.getPlayer().getName()));
                }
            }
            if (Option.REWARDS_MONEY.getAsDouble() != 0) {
                Util.depositPlayer(player.getPlayer(), Option.REWARDS_MONEY.getAsDouble());
            }
            if (Option.REWARDS_MESSAGE.get() != null) {
                player.send(Option.REWARDS_MESSAGE.get());
            }
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
        block.getWorld().getChunkAt(block);

        if (data instanceof Fence || data instanceof Wall) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data, false); // todo maybe add packet block change to instantly update it?
        }
        player.getPlayer().sendBlockChange(block.getLocation(), data);
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
        switch (direction) { // cos/sin system works clockwise with north on top, explanation: https://imgur.com/t2SFWc9
            default: // east
                // - 1/2 pi to 1/2 pi
                return new double[] { -0.5 * range, 0.5 * range };
            case WEST:
                // 1/2 pi to -1/2 pi
                return new double[] { 0.5 * range, -0.5 * range };
            case NORTH:
                // pi to 0
                return new double[] { range, 0 };
            case SOUTH:
                // 0 to pi
                return new double[] { 0, range };
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
        lastStandingPlayerLocation = spawn.clone();
        blockSpawn = block.clone();
        mostRecentBlock = block.clone();
        generate(player.blockLead);
    }

    // Generates in a loop
    public void generate(int amount) {
        for (int i = 0; i < amount; i++) {
            generate();
        }
    }

//    public static class InventoryHandler {
//
//        protected final ParkourPlayer pp;
//        protected final Player player;
//
//        public InventoryHandler(ParkourPlayer pp) {
//            this.pp = pp;
//            this.player = pp.getPlayer();
//        }
//
//        public void menu(String... optDisabled) {
//            InventoryBuilder builder = new InventoryBuilder(pp, 3, getInventoryName("general.menu")).open();
//            InventoryBuilder lead = new InventoryBuilder(pp, 3, getInventoryName("options.lead")).open();
//            InventoryBuilder timeofday = new InventoryBuilder(pp, 3, getInventoryName("options.time")).open();
//            InventoryBuilder language = new InventoryBuilder(pp, 3, getInventoryName("options.language")).open();
//            Configuration config = WITP.getConfiguration();
//            ItemStack close = config.getFromItemData(pp.locale, "general.close");
//
//            List<String> disabled = Arrays.asList(optDisabled);
//
//
// // Doesn't use if/else because every value needs to be checked
//            int itemCount = 9;
//            if (!checkOptions("styles", "witp.style", disabled)) itemCount--;             // 1
//            if (!checkOptions("lead", "witp.lead", disabled)) itemCount--;                // 2
//            if (!checkOptions("time", "witp.time", disabled)) itemCount--;                // 3
//            if (!checkOptions("difficulty", "witp.difficulty", disabled)) itemCount--;    // 4
//            if (!checkOptions("particles", "witp.particles", disabled)) itemCount--;      // 5
//            if (!checkOptions("scoreboard", "witp.scoreboard", disabled) && Option.SCOREBOARD.get()) itemCount--; // 6
//            if (!checkOptions("death-msg", "witp.fall", disabled)) itemCount--;           // 7
//            if (!checkOptions("special", "witp.special", disabled)) itemCount--;          // 8
//            if (!checkOptions("structure", "witp.structures", disabled)) itemCount--;     // 9
//
//            InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(itemCount, 1);
//            if (checkOptions("styles", "witp.style", disabled)) {
//                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.styles", pp.style), (t, e) -> {
//                    List<StyleType> styleTypes = WITP.getRegistry().getStyleTypes();
//                    if (styleTypes.size() == 1) {
//                        styleMenu(styleTypes.get(0), optDisabled);
//                    } else {
//                        String type = ChatColor.stripColor(e.getItemMeta().getDisplayName().toLowerCase());
//
//                    }
//                });
//            }
//            if (checkOptions("lead", "witp.lead", disabled)) {
//                List<Integer> possible = Option.POSSIBLE_LEADS;
//                InventoryBuilder.DynamicInventory dynamicLead = new InventoryBuilder.DynamicInventory(possible.size(), 1);
//                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.lead", Integer.toString(pp.blockLead)), (t, e) -> {
//                    for (Integer integer : possible) {
//                        lead.setItem(dynamicLead.next(), new Item(Material.PAPER, "&b&l" + integer).build(), (t2, e2) -> {
//                            if (e2.getItemMeta() != null) {
//                                pp.blockLead = Integer.parseInt(ChatColor.stripColor(e2.getItemMeta().getDisplayName()));
//                                pp.sendTranslated("selected-block-lead", Integer.toString(pp.blockLead));
//                            }
//                        });
//                    }
//                    lead.setItem(26, close, (t2, e2) -> menu(optDisabled));
//                    lead.build();
//                });
//            }
//            if (checkOptions("time", "witp.time", disabled)) {
//                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.time", pp.time.toLowerCase()), (t, e) -> {
//                    int i = 11;
//                    List<String> times = getOptionValues("time");
//                    if (times.size() != 5) {
//                        Logging.stack("Time translation values are incomplete!",
//                                "Make sure your translations are correct or delete your items-v3.yml file", null);
//                        pp.send("&4&l> &cThere was an error while handling changing that option!");
//                        return;
//                    }
//                    for (String time : times) {
//                        timeofday.setItem(i, new Item(Material.PAPER, "&b&l" + time).build(), (t2, e2) -> {
//                            if (e2.getItemMeta() != null) {
//                                String name = ChatColor.stripColor(e2.getItemMeta().getDisplayName());
//                                pp.time = name;
//                                pp.sendTranslated("selected-time", time.toLowerCase());
//                                pp.getPlayer().setPlayerTime(pp.getTime(name), false);
//                            }
//                        });
//                        i++;
//                    }
//                    timeofday.setItem(26, close, (t2, e2) -> menu(optDisabled));
//                    timeofday.build();
//                });
//            }
//            ItemStack item;
//            if (checkOptions("difficulty", "witp.difficulty", disabled)) {
//                builder.setItem(dynamic.next(),
//                        config.getFromItemData(pp.locale, "options.difficulty", "&a" + pp.calculateDifficultyScore() + "/1.0"),
//                        (t2, e2) -> difficultyMenu());
//            }
//            if (checkOptions("particles", "witp.particles", disabled)) {
//                String particlesString = Boolean.toString(pp.useParticlesAndSound);
//                item = config.getFromItemData(pp.locale, "options.particles", normalizeBoolean(Util.colorBoolean(particlesString)));
//                item.setType(pp.useParticlesAndSound ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//                builder.setItem(dynamic.next(), item, (t2, e2) -> {
//                    pp.useParticlesAndSound = !pp.useParticlesAndSound;
//                    pp.sendTranslated("selected-particles", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
//                    menu(optDisabled);
//                });
//            }
//            if (checkOptions("scoreboard", "witp.scoreboard", disabled) && Option.SCOREBOARD.get()) {
//                String scoreboardString = Boolean.toString(pp.showScoreboard);
//                item = config.getFromItemData(pp.locale, "options.scoreboard", normalizeBoolean(Util.colorBoolean(scoreboardString)));
//                item.setType(pp.showScoreboard ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//                builder.setItem(dynamic.next(), item, (t2, e2) -> {
//                    pp.showScoreboard = !pp.showScoreboard;
//                    if (pp.showScoreboard) {
//                        pp.setBoard(new FastBoard(player));
//                        pp.updateScoreboard();
//                    } else {
//                        if (pp.getBoard() != null) {
//                            pp.getBoard().delete();
//                        }
//                    }
//                    pp.sendTranslated("selected-scoreboard", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(scoreboardString))));
//                    menu(optDisabled);
//                });
//            }
//            if (checkOptions("death-msg", "witp.fall", disabled)) {
//                String deathString = Boolean.toString(pp.showFallMessage);
//                item = config.getFromItemData(pp.locale, "options.death-msg", normalizeBoolean(Util.colorBoolean(deathString)));
//                item.setType(pp.showFallMessage ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//                builder.setItem(dynamic.next(), item, (t2, e2) -> {
//                    pp.showFallMessage = !pp.showFallMessage;
//                    pp.sendTranslated("selected-fall-message", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
//                    menu(optDisabled);
//                });
//            }
//            if (checkOptions("special", "witp.special", disabled)) {
//                String specialString = Boolean.toString(pp.useSpecialBlocks);
//                item = config.getFromItemData(pp.locale, "options.special", normalizeBoolean(Util.colorBoolean(specialString)));
//                item.setType(pp.useSpecialBlocks ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//                builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("special", optDisabled));
//            }
//            if (checkOptions("structure", "witp.structures", disabled)) {
//                String structuresString = Boolean.toString(pp.useSchematic);
//                item = config.getFromItemData(pp.locale, "options.structure", normalizeBoolean(Util.colorBoolean(structuresString)));
//                item.setType(pp.useSchematic ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//                builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("structure", optDisabled));
//            }
//
//            if (checkOptions("gamemode", "witp.gamemode", disabled)) {
//                builder.setItem(18, WITP.getConfiguration().getFromItemData(pp.locale, "options.gamemode"), (t2, e2) -> pp.gamemode());
//            }
//            if (checkOptions("leaderboard", "witp.leaderboard", disabled)) {
//                Integer score = ParkourUser.highScores.get(pp.uuid);
//                builder.setItem(19, WITP.getConfiguration().getFromItemData(pp.locale, "options.leaderboard",
//                        pp.getTranslated("your-rank", Integer.toString(ParkourUser.getRank(pp.uuid)), Integer.toString(score == null ? 0 : score))), (t2, e2) -> {
//                    ParkourPlayer.leaderboard(pp, player, 1);
//                    player.closeInventory();
//                });
//            }
//            if (checkOptions("language", "witp.language", disabled)) {
//                builder.setItem(22, WITP.getConfiguration().getFromItemData(pp.locale, "options.language", pp.locale), (t2, e2) -> {
//                    List<String> langs = Option.LANGUAGES.get();
//                    InventoryBuilder.DynamicInventory dynamic1 = new InventoryBuilder.DynamicInventory(langs.size(), 1);
//                    for (String langName : langs) {
//                        language.setItem(dynamic1.next(), new Item(Material.PAPER, "&c" + langName).build(), (t3, e3) -> {
//                            pp.lang = langName;
//                            pp.locale = langName;
//                            pp.sendTranslated("selected-language", langName);
//                        });
//                    }
//                    language.setItem(26, close, (t3, e3) -> menu(optDisabled));
//                    language.build();
//                });
//            }
//            builder.setItem(26, WITP.getConfiguration().getFromItemData(pp.locale, "general.quit"), (t2, e2) -> {
//                player.closeInventory();
//                try {
//                    pp.sendTranslated("left");
//                    ParkourPlayer.unregister(pp, true, true, true);
//                } catch (IOException | InvalidStatementException ex) {
//                    Logging.stack("Error while unregistering player " + player.getName(),
//                            "Please try again or report this error to the developer!", ex);
//                }
//            });
//            builder.setItem(25, close, (t2, e2) -> player.closeInventory());
//            builder.build();
//        }
//
//        private String getInventoryName(String type) {
//            return ChatColor.stripColor(WITP.getConfiguration().getString("items", "locale." + pp.locale + "." + type.toLowerCase() + ".name"));
//        }
//
//        private List<String> getOptionValues(String option) {
//            Configuration config = WITP.getConfiguration();
//            List<String> values = config.getStringList("items", "locale." + pp.locale + ".options." + option.toLowerCase() + ".values");
//            if (values == null) {
//                Logging.warn("Didn't find any values for option '" + "locale." + pp.locale + ".options." + option.toLowerCase() + ".values'");
//                return Collections.emptyList();
//            }
//            return values;
//        }
//
//        private void stylesMenu(String... optDisabled) {
//            Configuration config = WITP.getConfiguration();
//            InventoryBuilder styling = new InventoryBuilder(pp, 3, getInventoryName("options.styles")).open();
//            ItemStack close = config.getFromItemData(pp.locale, "general.close");
//            throw new IllegalStateException();
//        }
//
//        private void styleMenu(StyleType type, String... optDisabled) {
//            Configuration config = WITP.getConfiguration();
//            InventoryBuilder styling = new InventoryBuilder(pp, 3, getInventoryName("options.styles")).open();
//            ItemStack close = config.getFromItemData(pp.locale, "general.close");
//
//            int i = 0;
//            for (String style : type.styles.keySet()) {
//                if (i == 26) {
//                    Logging.error("There are too many styles to display!");
//                    return;
//                }
//                if (Option.PERMISSIONS_STYLES.get() && pp.checkPermission("witp.styles." + style)) {
//                    continue;
//                }
//                styling.setItem(i, new Item(type.get(style), "&b&l" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
//                    String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
//                    pp.style = selected;
//                    pp.sendTranslated("selected-style", selected);
//                });
//                i++;
//            }
//            styling.setItem(26, close, (t2, e2) -> menu(optDisabled));
//            styling.build();
//        }
//
//        private void difficultyMenu(String... optDisabled) {
//            // Some important stuff
//            Configuration config = WITP.getConfiguration();
//            InventoryBuilder difficulty = new InventoryBuilder(pp, 3, getInventoryName("difficulty")).open();
//            ItemStack close = config.getFromItemData(pp.locale, "general.close");
//
//            InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(2, 1);
//
//            // Adaptive difficulty
//            String string = Boolean.toString(pp.useScoreDifficulty);
//            ItemStack item = config.getFromItemData(pp.locale, "options.adaptive-difficulty", normalizeBoolean(Util.colorBoolean(string)));
//            item.setType(pp.useScoreDifficulty ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
//
//            if (checkOptions("adaptive-difficulty", "witp.adaptive-difficulty", Arrays.asList(optDisabled))) {
//                difficulty.setItem(dynamic.next(), item, (t3, e3) -> askReset("difficulty"));
//            }
//
//            // Schematic difficulty
//            string = Util.parseDifficulty(pp.schematicDifficulty);
//            item = config.getFromItemData(pp.locale, "options.schematic-difficulty", string);
//            if (checkOptions("schematic-difficulty", "witp.schematic-difficulty", Arrays.asList(optDisabled))) {
//                difficulty.setItem(dynamic.next(), item, (t3, e3) -> schematicDifficultyMenu(optDisabled));
//            }
//
//            difficulty.setItem(26, close, (t3, e3) -> menu(optDisabled));
//            difficulty.build();
//        }
//
//        private void schematicDifficultyMenu(String... optDisabled) {
//            // Some important stuff
//            Configuration config = WITP.getConfiguration();
//            InventoryBuilder difficulty = new InventoryBuilder(pp, 3, getInventoryName("schematic-difficulty")).open();
//            ItemStack close = config.getFromItemData(pp.locale, "general.close");
//
//            InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(4, 1);
//
//            // All schematic difficulties
//            List<String> name = getOptionValues("schematic-difficulty");
//            difficulty.setItem(dynamic.next(),
//                    new Item(Material.LIME_WOOL, "&a&l" + Util.capitalizeFirst(name.get(0)))
//                            .glowing(pp.schematicDifficulty == 0.3)
//                            .build(),
//                    (t3, e3) -> askReset("e-difficulty"));
//            difficulty.setItem(dynamic.next(),
//                    new Item(Material.GREEN_WOOL,
//                            "&2&l" + Util.capitalizeFirst(name.get(1)))
//                            .glowing(pp.schematicDifficulty == 0.5)
//                            .build(),
//                    (t3, e3) -> askReset("m-difficulty"));
//            difficulty.setItem(dynamic.next(),
//                    new Item(Material.ORANGE_WOOL, "&6&l" + Util.capitalizeFirst(name.get(2)))
//                            .glowing(pp.schematicDifficulty == 0.7)
//                            .build(),
//                    (t3, e3) -> askReset("h-difficulty"));
//            difficulty.setItem(dynamic.next(),
//                    new Item(Material.RED_WOOL, "&c&l" + Util.capitalizeFirst(name.get(3)))
//                            .glowing(pp.schematicDifficulty == 0.8)
//                            .build(),
//                    (t3, e3) -> askReset("vh-difficulty"));
//
//            difficulty.setItem(26, close, (t3, e3) -> difficultyMenu(optDisabled));
//            difficulty.build();
//        }
//
//        private void askReset(String item, String... optDisabled) {
//            if (pp.getGenerator().getScore() < 25) {
//                confirmReset(item, optDisabled);
//                return;
//            }
//            pp.sendTranslated("confirm");
//            ComponentBuilder builder = new ComponentBuilder()
//                    .append(Util.color("&a&l" + pp.getTranslated("true").toUpperCase()))
//                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/witp askreset " + item + " true"))
//                    .append(Util.color(" &8| " + pp.getTranslated("confirm-click")));
//            player.spigot().sendMessage(builder.create());
//            player.closeInventory();
//        }
//
//        private boolean checkOptions(String option, @Nullable String perm, List<String> disabled) {
//            boolean enabled = WITP.getConfiguration().getFile("items").getBoolean("items.options." + option + ".enabled");
//            if (!enabled || disabled.contains(option)) {
//                return false;
//            } else {
//                return pp.checkPermission(perm);
//            }
//        }
//
//        public void confirmReset(String item, String... optDisabled) {
//            List<String> name = getOptionValues("schematic-difficulty");
//            if (name.size() != 4 && item.contains("-difficulty")) {
//                Logging.stack("Schematic difficulty translation values are incomplete!",
//                        "Make sure your translations are correct or delete your items-v3.yml file", null);
//                pp.send("&4&l> &cThere was an error while handling changing that option!");
//                return;
//            }
//            switch (item) {
//                case "structure":
//                    pp.useSchematic = !pp.useSchematic;
//                    pp.sendTranslated("selected-structures", normalizeBoolean(Util.colorBoolean(Boolean.toString(pp.useSchematic))));
//                    menu(optDisabled);
//                    break;
//                case "special":
//                    pp.useSpecialBlocks = !pp.useSpecialBlocks;
//                    pp.sendTranslated("selected-special-blocks", normalizeBoolean(Util.colorBoolean(Boolean.toString(pp.useSpecialBlocks))));
//                    menu(optDisabled);
//                    break;
//                case "e-difficulty":
//                    pp.schematicDifficulty = 0.3;
//                    pp.sendTranslated("selected-structure-difficulty", "&a" + name.get(0));
//                    schematicDifficultyMenu(optDisabled);
//                    break;
//                case "m-difficulty":
//                    pp.schematicDifficulty = 0.5;
//                    pp.sendTranslated("selected-structure-difficulty", "&e" + name.get(1));
//                    schematicDifficultyMenu(optDisabled);
//                    break;
//                case "h-difficulty":
//                    pp.schematicDifficulty = 0.7;
//                    pp.sendTranslated("selected-structure-difficulty", "&6" + name.get(2));
//                    schematicDifficultyMenu(optDisabled);
//                    break;
//                case "vh-difficulty":
//                    pp.schematicDifficulty = 0.8;
//                    pp.sendTranslated("selected-structure-difficulty", "&c" + name.get(3));
//                    schematicDifficultyMenu(optDisabled);
//                    break;
//                case "difficulty":
//                    pp.useScoreDifficulty = !pp.useScoreDifficulty;
//                    pp.sendTranslated("selected-difficulty", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(Boolean.toString(pp.useScoreDifficulty)))));
//                    difficultyMenu(optDisabled);
//                    break;
//            }
//        }
//
//        /**
//         * Makes a boolean readable for normal players
//         *
//         * @param   value
//         *          The value
//         *
//         * @return true -> yes, false -> no
//         */
//        private String normalizeBoolean(String value) {
//            return value.replace("true", pp.getTranslated("true")).replace("false", pp.getTranslated("false"));
//        }
//    }
}