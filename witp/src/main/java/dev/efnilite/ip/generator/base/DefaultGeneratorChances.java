package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.GeneratorOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.session.Session;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * An intermediary class to reduce the amount of clutter in the {@link dev.efnilite.ip.generator.DefaultGenerator} class.
 * This class is mostly used to determine all jump chances.
 */
public abstract class DefaultGeneratorChances extends ParkourGenerator {

    /**
     * The player
     */
    public ParkourPlayer player;

    /**
     * The chances of which distance the jump should have
     */
    public final HashMap<Integer, Integer> distanceChances;

    /**
     * Variable to determine how much the chance should be of a jump type, depending on the player's score
     */
    public final HashMap<Integer, Double> adaptiveDistanceChances;

    /**
     * The chances of which height the jump should have
     */
    public final HashMap<Integer, Integer> heightChances;

    /**
     * The chances of which type of special jump
     */
    public final HashMap<Integer, Integer> specialChances;

    /**
     * The chances of default jump types: schematic, 'special' (ice, etc.) or normal
     */
    public final HashMap<Integer, Integer> defaultChances;

    public DefaultGeneratorChances(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);

        this.player = session.getPlayers().get(0);
        this.distanceChances = new HashMap<>();
        this.heightChances = new HashMap<>();
        this.specialChances = new HashMap<>();
        this.defaultChances = new HashMap<>();
        this.adaptiveDistanceChances = new HashMap<>();

        calculateChances();
    }

    /**
     * Whether the option is present
     *
     * @param   option
     *          The option
     *
     * @return true if yes, false if not.
     */
    public boolean option(GeneratorOption option) {
        return generatorOptions.contains(option);
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
        if (profile.getValue("useScoreDifficulty").asBoolean() && option(GeneratorOption.DISABLE_ADAPTIVE)) {

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
}