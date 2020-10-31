package dev.efnilite.witp.generator;

import dev.efnilite.witp.ParkourPlayer;
import dev.efnilite.witp.WITP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class that generates the parkour, which each {@link dev.efnilite.witp.ParkourPlayer} has
 */
public class ParkourGenerator {

    private Location lastSpawn;
    private final ParkourPlayer player;
    private HashMap<Integer, Integer> defaultChances;
    private HashMap<Integer, Integer> normalChances;

    public ParkourGenerator(ParkourPlayer player) {
        this.player = player;
        this.lastSpawn = player.getPlayer().getLocation().clone().toCenterLocation();
        this.defaultChances = new HashMap<>();
        this.normalChances = new HashMap<>();
    }

    public void generateNext() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (defaultChances.size() == 0) {
            // Counting with % above 100%
            defaultChances = new HashMap<>();
            int index = 0;
            for (int i = 0; i < GeneratorChance.NORMAL; i++) {
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

                if (normalChances.size() == 0) {
                    normalChances = new HashMap<>();
                    int index = 0;
                    for (int i = 0; i < GeneratorChance.NORMAL_ONE_BLOCK; i++) {
                        normalChances.put(index, 1);
                        index++;
                    }
                    for (int i = 0; i < GeneratorChance.NORMAL_TWO_BLOCK; i++) {
                        normalChances.put(index, 2);
                        index++;
                    }
                    for (int i = 0; i < GeneratorChance.NORMAL_THREE_BLOCK; i++) {
                        normalChances.put(index, 3);
                        index++;
                    }
                    for (int i = 0; i < GeneratorChance.NORMAL_FOUR_BLOCK; i++) {
                        normalChances.put(index, 4);
                        index++;
                    }
                }

                int gap = normalChances.get(random.nextInt(normalChances.size())) + 1;
                lastSpawn.getBlock().setType(Material.LIGHT_BLUE_WOOL);
                List<Block> possible = getPossible(gap);
                Block chosen = possible.get(random.nextInt(possible.size() - 1));
                chosen.setType(Material.BLUE_WOOL);
                lastSpawn = chosen.getLocation().clone().toCenterLocation();

                break;
            case 1:
                break;
        }
    }

    private List<Block> getPossible(double radius) {
        List<Block> possible = new ArrayList<>();
        World world = lastSpawn.getWorld();
        double increment = (2 * Math.PI) / 50;

        for (int i = 0; i < 50; i++) {
            double angle = i * increment;
            double x = lastSpawn.getX() + (radius * Math.cos(angle));
            double z = lastSpawn.getZ() + (radius * Math.sin(angle));
            Block block = new Location(world, x, lastSpawn.getY(), z).getBlock();
            if (lastSpawn.clone().subtract(block.getLocation()).toVector().getX() < 0 &&
                    block.getLocation().toCenterLocation().distance(lastSpawn) <= 5) {
                possible.add(block);
            }
        }
        return possible;
    }

    public static class GeneratorChance {

        public static int NORMAL;
        public static int STRUCTURES;

        public static int NORMAL_ONE_BLOCK;
        public static int NORMAL_TWO_BLOCK;
        public static int NORMAL_THREE_BLOCK;
        public static int NORMAL_FOUR_BLOCK;

        public static void init() {
            FileConfiguration file = WITP.getConfiguration().getFile("generation");
            NORMAL = file.getInt("generation.normal-jump.chance");
            STRUCTURES = file.getInt("generation.structures.chance");

            NORMAL_ONE_BLOCK = file.getInt("generation.normal-jump.1-block");
            NORMAL_TWO_BLOCK = file.getInt("generation.normal-jump.2-block");
            NORMAL_THREE_BLOCK = file.getInt("generation.normal-jump.3-block");
            NORMAL_FOUR_BLOCK = file.getInt("generation.normal-jump.4-block");
        }
    }
}