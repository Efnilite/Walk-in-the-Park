package dev.efnilite.ip.generator.base;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.session.SingleSession;
import dev.efnilite.ip.util.config.Option;
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
    protected ParkourPlayer player;

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

    public DefaultGeneratorChances(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);

        player = session.getPlayers().get(0);
        if (!(session instanceof SingleSession)) {
            throw new IllegalArgumentException("Session is not a SingleSession");
        }

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
    protected boolean option(GeneratorOption option) {
        return generatorOptions.contains(option);
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
        for (int i = 0; i < Option.SPECIAL_ICE.get(); i++) {
            specialChances.put(percentage, 0);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_SLAB.get(); i++) {
            specialChances.put(percentage, 1);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_PANE.get(); i++) {
            specialChances.put(percentage, 2);
            percentage++;
        }
        for (int i = 0; i < Option.SPECIAL_FENCE.get(); i++) {
            specialChances.put(percentage, 3);
            percentage++;
        }
    }

    /**
     * Calculates chances for adaptive distances
     */
    protected void calculateAdaptiveDistance() {
        adaptiveDistanceChances.clear();

        double multiplier = Option.MULTIPLIER.getAsDouble();
        adaptiveDistanceChances.put(1, (Option.MAXED_ONE_BLOCK.get() - Option.NORMAL_ONE_BLOCK.get()) / multiplier);
        adaptiveDistanceChances.put(2, (Option.MAXED_TWO_BLOCK.get() - Option.NORMAL_TWO_BLOCK.get()) / multiplier);
        adaptiveDistanceChances.put(3, (Option.MAXED_THREE_BLOCK.get() - Option.NORMAL_THREE_BLOCK.get()) / multiplier);
        adaptiveDistanceChances.put(4, (Option.MAXED_FOUR_BLOCK.get() - Option.NORMAL_FOUR_BLOCK.get()) / multiplier);
    }

    /**
     * Calculates the chances of default jump types
     */
    protected void calculateDefault() {
        defaultChances.clear();

        int percentage = 0;
        for (int i = 0; i < Option.NORMAL.get(); i++) { // normal
            defaultChances.put(percentage, 0);
            percentage++;
        }
        if (!option(GeneratorOption.DISABLE_SCHEMATICS)) { // schematics
            for (int i = 0; i < Option.SCHEMATICS.get(); i++) {
                defaultChances.put(percentage, 1);
                percentage++;
            }
        }
        if (!option(GeneratorOption.DISABLE_SPECIAL)) { // special
            for (int i = 0; i < Option.SPECIAL.get(); i++) {
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
        for (int i = 0; i < Option.NORMAL_UP.get(); i++) {
            heightChances.put(percentage, 1);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_LEVEL.get(); i++) {
            heightChances.put(percentage, 0);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_DOWN.get(); i++) {
            heightChances.put(percentage, -1);
            percentage++;
        }
        for (int i = 0; i < Option.NORMAL_DOWN2.get(); i++) {
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
        int one = Option.MAXED_ONE_BLOCK.get();
        int two = Option.MAXED_TWO_BLOCK.get();
        int three = Option.MAXED_THREE_BLOCK.get();
        int four = Option.MAXED_FOUR_BLOCK.get();

        // If the player uses difficulty, slowly increase the chances of harder jumps (depends on user settings though)
        if (player.useScoreDifficulty && option(GeneratorOption.DISABLE_ADAPTIVE)) {
            if (score <= Option.MULTIPLIER.getAsDouble()) {
                one = (int) (Option.NORMAL_ONE_BLOCK.get() + (adaptiveDistanceChances.get(1) * score));
                two = (int) (Option.NORMAL_TWO_BLOCK.get() + (adaptiveDistanceChances.get(2) * score));
                three = (int) (Option.NORMAL_THREE_BLOCK.get() + (adaptiveDistanceChances.get(3) * score));
                four = (int) (Option.NORMAL_FOUR_BLOCK.get() + (adaptiveDistanceChances.get(4) * score));
            }
        } else {
            one = Option.NORMAL_ONE_BLOCK.get();
            two = Option.NORMAL_TWO_BLOCK.get();
            three = Option.NORMAL_THREE_BLOCK.get();
            four = Option.NORMAL_FOUR_BLOCK.get();
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