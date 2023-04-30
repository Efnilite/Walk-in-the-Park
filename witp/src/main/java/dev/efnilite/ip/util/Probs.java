package dev.efnilite.ip.util;

import java.util.LinkedHashMap;
import java.util.Map;
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
     * @param distribution A map where each key is mapped to a probability.
     * @param <K>          The key type.
     * @return A random item from the list, based on the probabilities.
     */
    public static <K> K random(Map<K, Double> distribution) {
        double total = 0;

        Map<K, Double> cumulative = new LinkedHashMap<>();
        for (Map.Entry<K, Double> entry : distribution.entrySet()) {
            total += entry.getValue();
            cumulative.put(entry.getKey(), total);
        }

        double random = random() * total;
        for (Map.Entry<K, Double> entry : cumulative.entrySet()) {
            if (entry.getValue() >= random) {
                return entry.getKey();
            }
        }
        return distribution.keySet().stream().findFirst().orElseThrow();
    }
}