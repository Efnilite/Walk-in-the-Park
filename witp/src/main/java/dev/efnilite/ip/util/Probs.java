package dev.efnilite.ip.util;

import java.util.concurrent.ThreadLocalRandom;

public final class Probs {

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * @return A random chance with range 0 (inclusive) to 1 (exclusive).
     */
    public static double random() {
        return random.nextDouble(1);
    }

    /**
     * @param mean The average value.
     * @param sd   The standard deviation.
     * @param x    The x-coordinate.
     * @return The normal distributed chance at x.
     */
    public static double normalpdf(double mean, double sd, double x) {
        double a = (x - mean) / sd;

        return (1 / (sd * Math.sqrt(2 * Math.PI))) * Math.exp(-0.5 * (a * a));
    }
}