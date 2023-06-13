package dev.efnilite.ip.generator;

import dev.efnilite.ip.util.Probs;
import dev.efnilite.vilib.util.Colls;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a random offset (the sidestep) for a parkour block.
 * Source: <a href="https://i.imgur.com/RPevMfX.png">here</a>.
 */
public class JumpOffsetGenerator {

    private final int maxOffset;

    /**
     * Constructor.
     *
     * @param y The height difference.
     */
    public JumpOffsetGenerator(int y, int distance) {
        this.maxOffset = switch (y) {
            case 1 -> switch (distance) {
                case 1 -> 4;
                case 2 -> 3;
                default -> 2;
            };
            case 0 -> switch (distance) {
                case 1, 2 -> 4;
                case 3 -> 3;
                default -> 2;
            };
            case -1 -> switch (distance) {
                case 1, 2 -> 5;
                case 3 -> 4;
                default -> 3;
            };
            case -2 -> switch (distance) {
                case 1, 2 -> 5;
                default -> 4;
            };
            default -> throw new IllegalArgumentException("Unknown jump height difference %d".formatted(y));
        };
    }

    /**
     * @param mean              The mean (average) value for the offset. Usually 0 to avoid parkour going only left or right.
     * @param standardDeviation The standard deviation.
     * @return A random jump-able offset.
     */
    public int getRandomOffset(double mean, double standardDeviation) {
        Map<Integer, Double> distribution = Colls.range(-maxOffset, maxOffset + 1)
                .stream()
                .collect(Collectors.toMap(offset -> offset, offset -> Probs.normalpdf(mean, standardDeviation, offset)));

        return Probs.random(distribution);
    }
}