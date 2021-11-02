package dev.efnilite.witp.generator;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.events.BlockGenerateEvent;
import dev.efnilite.witp.events.PlayerFallEvent;
import dev.efnilite.witp.events.PlayerScoreEvent;
import dev.efnilite.witp.generator.base.DefaultGeneratorBase;
import dev.efnilite.witp.generator.base.GeneratorOption;
import dev.efnilite.witp.generator.base.ParkourGenerator;
import dev.efnilite.witp.generator.subarea.Direction;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.player.ParkourUser;
import dev.efnilite.witp.schematic.Schematic;
import dev.efnilite.witp.schematic.SchematicAdjuster;
import dev.efnilite.witp.schematic.SchematicCache;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.particle.ParticleData;
import dev.efnilite.witp.util.particle.Particles;
import dev.efnilite.witp.util.sql.InvalidStatementException;
import dev.efnilite.witp.util.task.Tasks;
import fr.mrmicky.fastboard.FastBoard;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class DefaultGenerator extends DefaultGeneratorBase {

    private BukkitRunnable task;
    public DefaultGenerator.InventoryHandler handler;

    protected int totalScore;
    protected int structureCooldown;
    protected boolean deleteStructure;
    protected boolean stopped;
    protected boolean waitForSchematicCompletion;
    protected Direction heading;

    protected Location lastSpawn;
    protected Location lastPlayer;
    protected Location previousSpawn;
    protected Location latestLocation; // to disallow 1 block infinite point glitch

    protected Location playerSpawn;
    protected Location blockSpawn;
    protected List<Block> structureBlocks;

    protected final Queue<Block> generatedHistory;
    protected final LinkedHashMap<String, Integer> buildLog;

    protected static final ParticleData<?> PARTICLE_DATA = new ParticleData<>(Particle.SPELL_INSTANT, null, 10, 0, 0, 0, 0);

    /**
     * Creates a new ParkourGenerator instance
     *
     * @param player The player associated with this generator
     */
    public DefaultGenerator(@NotNull ParkourPlayer player, GeneratorOption... generatorOptions) {
        super(player, generatorOptions);
        Verbose.verbose("Init of DefaultGenerator of " + player.getPlayer().getName());
        calculateChances();

        this.handler = new InventoryHandler(player);
        this.heading = Option.HEADING;

        this.score = 0;
        this.totalScore = 0;
        this.stopped = false;
        this.waitForSchematicCompletion = false;
        this.structureCooldown = 20;
        this.lastSpawn = player.getLocation().clone();
        this.lastPlayer = lastSpawn.clone();
        this.latestLocation = lastSpawn.clone();
        this.generatedHistory = new LinkedList<>();
        this.buildLog = new LinkedHashMap<>();
        this.structureBlocks = new ArrayList<>();
        this.deleteStructure = false;
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
                tick();

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
                    heading = heading.turnRight(); // reverse heading
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
                int gap = distanceChances.get(random.nextInt(distanceChances.size())) + 1;

                BlockData material = player.randomMaterial().createBlockData();
                if (special == 1 && player.useSpecial) {
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
                if (possible.isEmpty()) {
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
                            PARTICLE_DATA.setSpeed(0.4).setSize(20).setOffsetX(0.5).setOffsetY(1).setOffsetZ(0.5);
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
                if (!files.isEmpty()) {
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
                double gapStructure = distanceChances.get(random.nextInt(distanceChances.size())) + 1;

                Location local2 = lastSpawn.clone();
                List<Block> possibleStructure = getPossible(gapStructure, 0);
                if (possibleStructure.isEmpty()) {
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
                if (structureBlocks == null || structureBlocks.isEmpty()) {
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

    public void altMenu() {
        // to be inherited
    }

    public boolean hasAltMenu() {
        return false;
    }

    @Override
    public void menu() {
        handler.menu();
    }

    private void addPoint() {
        score++;
        totalScore++;
        score();
        checkRewards();
    }

    private void checkRewards() {
        if (!Option.REWARDS) {
            return;
        }

        // Rewards
        HashMap<Integer, List<String>> scores = Option.REWARDS_SCORES;
        if (!scores.isEmpty() && scores.containsKey(score) && scores.get(score) != null) {
            List<String> commands = scores.get(score);
            if (commands != null) {
                if (Option.INVENTORY_HANDLING) {
                    rewardsLeaveList.addAll(commands);
                } else {
                    for (String command : commands) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player.getPlayer().getName()));
                    }
                }
            }
        }

        // Interval rewards
        if (Option.REWARDS_INTERVAL > 0 && totalScore % Option.REWARDS_INTERVAL == 0) {
            if (Option.INTERVAL_REWARDS_SCORES != null) {
                for (String command : Option.INTERVAL_REWARDS_SCORES) {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            command.replace("%player%", player.getPlayer().getName()));
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

    protected void deleteStructure() {
        for (Block block : structureBlocks) {
            block.setType(Material.AIR);
        }

        structureBlocks.clear();
        deleteStructure = false;
        structureCooldown = 20;
    }

    protected void setBlock(Block block, BlockData data) {
        if (data instanceof Fence || data instanceof Wall) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data);
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
    protected List<Block> getPossible(double radius, double dy) {
        List<Block> possible = new ArrayList<>();

        World world = lastSpawn.getWorld();
        Location base = lastSpawn.add(0, dy, 0); // adds y to the last spawned block

        int y = base.getBlockY();

        // the distance, adjusted to the height (dy)
        double heightGap = dy >= 0 ? Option.HEIGHT_GAP - dy : Option.HEIGHT_GAP - (dy + 1);

        // the range in which it should check for blocks (max 180 degrees, min 90 degrees)
        double range = option(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? Math.PI * 0.5 : Math.PI;

        double[] bounds = getBounds(heading, range);
        double startBound = bounds[0];
        double limitBound = bounds[1];

        double detail = radius * 4; // how many times it should check
        double increment = range / detail; // 180 degrees / amount of times it should check = the increment

        if (radius > 1) { // if the radius is 1, adding extra to the bounds might cause blocks to spawn on top of each other
            startBound += 1.5 * increment; // remove blocks on the same axis
            limitBound -= 1.5 * increment;
        } else if (radius < 1) {
            return getPossible(1, 0); // invalid radius, can't be below 1
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

    public static class InventoryHandler extends ParkourGenerator.InventoryHandler {

        public InventoryHandler(ParkourPlayer pp) {
            super(pp);
        }

        public void menu(String... optDisabled) {
            InventoryBuilder builder = new InventoryBuilder(pp, 3, "Customize").open();
            InventoryBuilder lead = new InventoryBuilder(pp, 3, "Lead").open();
            InventoryBuilder styling = new InventoryBuilder(pp, 3, "Parkour style").open();
            InventoryBuilder timeofday = new InventoryBuilder(pp, 3, "Time").open();
            InventoryBuilder language = new InventoryBuilder(pp, 3, "Language").open();
            Configuration config = WITP.getConfiguration();
            ItemStack close = config.getFromItemData(pp.locale, "general.close");

            List<String> disabled = Arrays.asList(optDisabled);

            // Check which items should be displayed, out of all available (this is how DynamicInventory works, and too lazy to change it)
            // Doesn't use if/else because every value needs to be checked
            int itemCount = 9;
            if (!checkOptions("styles", "witp.style", disabled)) itemCount--;             // 1
            if (!checkOptions("lead", "witp.lead", disabled)) itemCount--;                // 2
            if (!checkOptions("time", "witp.time", disabled)) itemCount--;                // 3
            if (!checkOptions("difficulty", "witp.difficulty", disabled)) itemCount--;    // 4
            if (!checkOptions("particles", "witp.particles", disabled)) itemCount--;      // 5
            if (!checkOptions("scoreboard", "witp.scoreboard", disabled) && Option.SCOREBOARD) itemCount--; // 6
            if (!checkOptions("death-msg", "witp.fall", disabled)) itemCount--;           // 7
            if (!checkOptions("special", "witp.special", disabled)) itemCount--;          // 8
            if (!checkOptions("structure", "witp.structures", disabled)) itemCount--;     // 9


            InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(itemCount, 1);
            if (checkOptions("styles", "witp.style", disabled)) {
                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.styles", pp.style), (t, e) -> {
                    List<String> pos = Util.getNode(WITP.getConfiguration().getFile("config"), "styles.list");
                    if (pos == null) {
                        Verbose.error("Error while trying to fetch possible styles from config.yml");
                        return;
                    }
                    int i = 0;
                    Random random = ThreadLocalRandom.current();
                    for (String style : pos) {
                        if (i == 26) {
                            Verbose.error("There are too many styles to display!");
                            return;
                        }
                        if (Option.PERMISSIONS_STYLES && pp.checkPermission("witp.styles." + style.toLowerCase())) {
                            continue;
                        }
                        List<Material> possible = pp.getPossibleMaterials(style);
                        if (possible == null) {
                            continue;
                        }
                        Material material = possible.get(random.nextInt(possible.size() - 1));
                        styling.setItem(i, new ItemBuilder(material, "&b&l" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
                            String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
                            pp.setStyle(selected);
                            pp.sendTranslated("selected-style", selected);
                        });
                        i++;
                    }
                    styling.setItem(26, close, (t2, e2) -> menu(optDisabled));
                    styling.build();
                });
            }
            if (checkOptions("lead", "witp.lead", disabled)) {
                List<Integer> possible = Option.POSSIBLE_LEADS;
                InventoryBuilder.DynamicInventory dynamicLead = new InventoryBuilder.DynamicInventory(possible.size(), 1);
                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.lead", Integer.toString(pp.blockLead)), (t, e) -> {
                    for (Integer integer : possible) {
                        lead.setItem(dynamicLead.next(), new ItemBuilder(Material.PAPER, "&b&l" + integer).build(), (t2, e2) -> {
                            if (e2.getItemMeta() != null) {
                                pp.blockLead = Integer.parseInt(ChatColor.stripColor(e2.getItemMeta().getDisplayName()));
                                pp.sendTranslated("selected-block-lead", Integer.toString(pp.blockLead));
                            }
                        });
                    }
                    lead.setItem(26, close, (t2, e2) -> menu(optDisabled));
                    lead.build();
                });
            }
            if (checkOptions("time", "witp.time", disabled)) {
                builder.setItem(dynamic.next(), config.getFromItemData(pp.locale, "options.time", pp.time.toLowerCase()), (t, e) -> {
                    List<String> pos = Arrays.asList("Day", "Noon", "Dawn", "Night", "Midnight");
                    int i = 11;
                    for (String time : pos) {
                        timeofday.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + time).build(), (t2, e2) -> {
                            if (e2.getItemMeta() != null) {
                                String name = ChatColor.stripColor(e2.getItemMeta().getDisplayName());
                                pp.time = name;
                                pp.sendTranslated("selected-time", time.toLowerCase());
                                pp.getPlayer().setPlayerTime(pp.getTime(name), false);
                            }
                        });
                        i++;
                    }
                    timeofday.setItem(26, close, (t2, e2) -> menu(optDisabled));
                    timeofday.build();
                });
            }
            ItemStack item;
            if (checkOptions("difficulty", "witp.difficulty", disabled)) {
                builder.setItem(dynamic.next(),
                        config.getFromItemData(pp.locale, "options.difficulty", "&a" + Util.parseDifficulty(pp.difficulty) + " &7(" + pp.calculateDifficultyScore() + "/1.0)"),
                        (t2, e2) -> difficultyMenu());
            }
            if (checkOptions("particles", "witp.particles", disabled)) {
                String particlesString = Boolean.toString(pp.useParticles);
                item = config.getFromItemData(pp.locale, "options.particles", normalizeBoolean(Util.colorBoolean(particlesString)));
                item.setType(pp.useParticles ? Material.GREEN_WOOL : Material.RED_WOOL);
                builder.setItem(dynamic.next(), item, (t2, e2) -> {
                    pp.useParticles = !pp.useParticles;
                    pp.sendTranslated("selected-particles", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
                    menu(optDisabled);
                });
            }
            if (checkOptions("scoreboard", "witp.scoreboard", disabled) && Option.SCOREBOARD) {
                String scoreboardString = Boolean.toString(pp.showScoreboard);
                item = config.getFromItemData(pp.locale, "options.scoreboard", normalizeBoolean(Util.colorBoolean(scoreboardString)));
                item.setType(pp.showScoreboard ? Material.GREEN_WOOL : Material.RED_WOOL);
                builder.setItem(dynamic.next(), item, (t2, e2) -> {
                    pp.showScoreboard = !pp.showScoreboard;
                    if (pp.showScoreboard) {
                        pp.setBoard(new FastBoard(player));
                        pp.updateScoreboard();
                    } else {
                        pp.getBoard().delete();
                    }
                    pp.sendTranslated("selected-scoreboard", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(scoreboardString))));
                    menu(optDisabled);
                });
            }
            if (checkOptions("death-msg", "witp.fall", disabled)) {
                String deathString = Boolean.toString(pp.showDeathMsg);
                item = config.getFromItemData(pp.locale, "options.death-msg", normalizeBoolean(Util.colorBoolean(deathString)));
                item.setType(pp.showDeathMsg ? Material.GREEN_WOOL : Material.RED_WOOL);
                builder.setItem(dynamic.next(), item, (t2, e2) -> {
                    pp.showDeathMsg = !pp.showDeathMsg;
                    pp.sendTranslated("selected-fall-message", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
                    menu(optDisabled);
                });
            }
            if (checkOptions("special", "witp.special", disabled)) {
                String specialString = Boolean.toString(pp.useSpecial);
                item = config.getFromItemData(pp.locale, "options.special", normalizeBoolean(Util.colorBoolean(specialString)));
                item.setType(pp.useSpecial ? Material.GREEN_WOOL : Material.RED_WOOL);
                builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("special", optDisabled));
            }
            if (checkOptions("structure", "witp.structures", disabled)) {
                String structuresString = Boolean.toString(pp.useStructure);
                item = config.getFromItemData(pp.locale, "options.structure", normalizeBoolean(Util.colorBoolean(structuresString)));
                item.setType(pp.useStructure ? Material.GREEN_WOOL : Material.RED_WOOL);
                builder.setItem(dynamic.next(), item, (t2, e2) -> askReset("structure", optDisabled));
            }

            if (checkOptions("gamemode", "witp.gamemode", disabled)) {
                builder.setItem(18, WITP.getConfiguration().getFromItemData(pp.locale, "options.gamemode"), (t2, e2) -> pp.gamemode());
            }
            if (checkOptions("leaderboard", "witp.leaderboard", disabled)) {
                Integer score = ParkourUser.highScores.get(pp.uuid);
                builder.setItem(19, WITP.getConfiguration().getFromItemData(pp.locale, "options.leaderboard",
                        pp.getTranslated("your-rank", Integer.toString(ParkourUser.getRank(pp.uuid)), Integer.toString(score == null ? 0 : score))), (t2, e2) -> {
                    ParkourPlayer.leaderboard(pp, player, 1);
                    player.closeInventory();
                });
            }
            if (checkOptions("language", "witp.language", disabled)) {
                builder.setItem(22, WITP.getConfiguration().getFromItemData(pp.locale, "options.language", pp.locale), (t2, e2) -> {
                    List<String> langs = Option.LANGUAGES;
                    InventoryBuilder.DynamicInventory dynamic1 = new InventoryBuilder.DynamicInventory(langs.size(), 1);
                    for (String langName : langs) {
                        language.setItem(dynamic1.next(), new ItemBuilder(Material.PAPER, "&c" + langName).build(), (t3, e3) -> {
                            pp.lang = langName;
                            pp.locale = langName;
                            pp.sendTranslated("selected-language", langName);
                        });
                    }
                    language.setItem(26, close, (t3, e3) -> menu(optDisabled));
                    language.build();
                });
            }
            builder.setItem(26, WITP.getConfiguration().getFromItemData(pp.locale, "general.quit"), (t2, e2) -> {
                player.closeInventory();
                try {
                    pp.sendTranslated("left");
                    ParkourPlayer.unregister(pp, true, true, true);
                } catch (IOException | InvalidStatementException ex) {
                    ex.printStackTrace();
                    Verbose.error("Error while trying to quit player " + player.getName());
                }
            });
            builder.setItem(25, close, (t2, e2) -> player.closeInventory());
            builder.build();
        }

        private void difficultyMenu() {
            Configuration config = WITP.getConfiguration();
            InventoryBuilder difficulty = new InventoryBuilder(pp, 3, "Difficulty").open();
            ItemStack close = config.getFromItemData(pp.locale, "general.close");

            InventoryBuilder.DynamicInventory dynamic1 = new InventoryBuilder.DynamicInventory(5, 1);
            String difficultyString = Boolean.toString(pp.useDifficulty);
            ItemStack diffSwitchItem = config.getFromItemData(pp.locale, "options.difficulty-switch", normalizeBoolean(Util.colorBoolean(difficultyString)));
            diffSwitchItem.setType(pp.useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL);
            int diffSlot = dynamic1.next();
            difficulty.setItem(diffSlot, diffSwitchItem, (t3, e3) -> {
                if (checkOptions("difficulty-switch", "witp.difficulty-switch", new ArrayList<>())) {
                    askReset("difficulty");
                }
            });
            difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.LIME_WOOL, "&a&l" + Util.capitalizeFirst(Util.parseDifficulty(0.3))).build(), (t3, e3) -> askReset("e-difficulty"));
            difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.GREEN_WOOL, "&2&l" + Util.capitalizeFirst(Util.parseDifficulty(0.5))).build(), (t3, e3) -> askReset("m-difficulty"));
            difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.ORANGE_WOOL, "&6&l" + Util.capitalizeFirst(Util.parseDifficulty(0.7))).build(), (t3, e3) -> askReset("h-difficulty"));
            difficulty.setItem(dynamic1.next(), new ItemBuilder(Material.RED_WOOL, "&c&l" + Util.capitalizeFirst(Util.parseDifficulty(0.8))).build(), (t3, e3) -> askReset("vh-difficulty"));

            difficulty.setItem(26, close, (t3, e3) -> pp.getGenerator().menu());
            difficulty.build();
        }

        private void askReset(String item, String... optDisabled) {
            if (pp.getGenerator().score < 25) {
                confirmReset(item, optDisabled);
                return;
            }
            pp.sendTranslated("confirm");
            ComponentBuilder builder = new ComponentBuilder()
                    .append(Util.color("&a&l" + pp.getTranslated("true").toUpperCase()))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/witp askreset " + item + " true"))
                    .append(Util.color(" &8| " + pp.getTranslated("confirm-click")));
            player.spigot().sendMessage(builder.create());
            player.closeInventory();
        }

        private boolean checkOptions(String option, @Nullable String perm, List<String> disabled) {
            boolean enabled = WITP.getConfiguration().getFile("items").getBoolean("items.options." + option + ".enabled");
            if (!enabled || disabled.contains(option)) {
                return false;
            } else {
                return pp.checkPermission(perm);
            }
        }

        public void confirmReset(String item, String... optDisabled) {
            switch (item) {
                case "structure":
                    pp.useStructure = !pp.useStructure;
                    pp.sendTranslated("selected-structures", normalizeBoolean(Util.colorBoolean(Boolean.toString(pp.useStructure))));
                    menu(optDisabled);
                    break;
                case "special":
                    pp.useSpecial = !pp.useSpecial;
                    pp.sendTranslated("selected-special-blocks", normalizeBoolean(Util.colorBoolean(Boolean.toString(pp.useSpecial))));
                    menu(optDisabled);
                    break;
                case "e-difficulty":
                    pp.difficulty = 0.3;
                    pp.sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(pp.difficulty));
                    break;
                case "m-difficulty":
                    pp.difficulty = 0.5;
                    pp.sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(pp.difficulty));
                    break;
                case "h-difficulty":
                    pp.difficulty = 0.7;
                    pp.sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(pp.difficulty));
                    break;
                case "vh-difficulty":
                    pp.difficulty = 0.8;
                    pp.sendTranslated("selected-structure-difficulty", "&c" + Util.parseDifficulty(pp.difficulty));
                    break;
                case "difficulty":
                    pp.useDifficulty = !pp.useDifficulty;
                    pp.sendTranslated("selected-difficulty", normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(Boolean.toString(pp.useDifficulty)))));
                    difficultyMenu();
                    break;
            }
        }

        /**
         * Makes a boolean readable for normal players
         *
         * @param   value
         *          The value
         *
         * @return true -> yes, false -> no
         */
        private String normalizeBoolean(String value) {
            return value.replace("true", pp.getTranslated("true")).replace("false", pp.getTranslated("false"));
        }
    }
}