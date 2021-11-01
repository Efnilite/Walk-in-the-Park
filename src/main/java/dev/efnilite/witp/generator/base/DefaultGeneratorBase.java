package dev.efnilite.witp.generator.base;

import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.player.ParkourPlayer;
import dev.efnilite.witp.util.config.Option;

import java.util.HashMap;

/**
 * Class to reduce the amount of mess in {@link DefaultGenerator}
 */
public class DefaultGeneratorBase extends ParkourGenerator {

    /**
     * The chances of which distance the jump should have
     */
    protected final HashMap<Integer, Integer> distanceChances;

    /**
     * Variable to determine how much the chance should be of a jump type, depending on the player's score
     */
    protected final HashMap<Integer, Double> adaptiveDistanceChances;

    /**
     * The chances of which height the jump should have
     */
    protected final HashMap<Integer, Integer> heightChances;

    /**
     * The chances of which type of special jump
     */
    protected final HashMap<Integer, Integer> specialChances;

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    protected final HashMap<Integer, Integer> defaultChances;

    public DefaultGeneratorBase(ParkourPlayer player, GeneratorOption... generatorOptions) {
        super(player, generatorOptions);

        this.distanceChances = new HashMap<>();
        this.heightChances = new HashMap<>();
        this.specialChances = new HashMap<>();
        this.defaultChances = new HashMap<>();
        this.adaptiveDistanceChances = new HashMap<>();

        calculateChances();
    }

    /**
     * Calculates all chances for every variable
     */
    protected void calculateChances() {
        calculateAdaptiveDistance();
        calculateDefault();
        calculateHeight();
        calculateDistance();
        calculateSpecial();
    }

    /**
     * Calculates the chances of which type of special jump
     */
    protected void calculateSpecial() {
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
    protected void calculateAdaptiveDistance() {
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
    protected void calculateDefault() {
        defaultChances.clear();

        int percentage = 0;
        for (int i = 0; i < Option.NORMAL; i++) { // normal
            defaultChances.put(percentage, 0);
            percentage++;
        }
        if (!option(GeneratorOption.DISABLE_SCHEMATICS)) { // schematics
            for (int i = 0; i < Option.SCHEMATICS; i++) {
                defaultChances.put(percentage, 1);
                percentage++;
            }
        }
        if (!option(GeneratorOption.DISABLE_SPECIAL)) { // special
            for (int i = 0; i < Option.SPECIAL; i++) {
                defaultChances.put(percentage, 2);
                percentage++;
            }
        }
    }

    /**
     * Calculates the chances of height
     */
    protected void calculateHeight() {
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
    protected void calculateDistance() {
        distanceChances.clear();

        // The max percentages
        int one = Option.MAXED_ONE_BLOCK;
        int two = Option.MAXED_TWO_BLOCK;
        int three = Option.MAXED_THREE_BLOCK;
        int four = Option.MAXED_FOUR_BLOCK;

        // If the player uses difficulty, slowly increase the chances of harder jumps (depends on user settings though)
        if (player.useDifficulty) {
            if (score <= Option.MULTIPLIER) {
                one = (int) (Option.NORMAL_ONE_BLOCK + (adaptiveDistanceChances.get(1) * score));
                two = (int) (Option.NORMAL_TWO_BLOCK + (adaptiveDistanceChances.get(2) * score));
                three = (int) (Option.NORMAL_THREE_BLOCK + (adaptiveDistanceChances.get(3) * score));
                four = (int) (Option.NORMAL_FOUR_BLOCK + (adaptiveDistanceChances.get(4) * score));
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

    @Override
    public void reset(boolean regenerateBack) {

    }

    @Override
    public void start() {

    }

    @Override
    public void generate() {

    }

    @Override
    public void menu() {

    }
}