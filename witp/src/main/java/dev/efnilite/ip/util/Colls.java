package dev.efnilite.ip.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Colls {

    /**
     * Creates a util class to simplify map threading.
     *
     * @param map The map.
     * @param <K> The key type.
     * @param <V> The value type.
     * @return A new {@link MapThreader}.
     */
    public static <K, V> MapThreader<K, V> thread(Map<K, V> map) {
        return new MapThreader<>(map);
    }

    public interface Threader<T> {

        /**
         * @return The value.
         */
        T get();

    }

    /**
     * Threads through maps. Modifications return {@link HashMap}.
     *
     * @param <K> The key type.
     * @param <V> The value type.
     */
    public static class MapThreader<K, V> implements Threader<Map<K, V>> {

        private Map<K, V> map;

        public MapThreader(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public Map<K, V> get() {
            return map;
        }

        /**
         * @param f The function to apply.
         * @return Threader with map, only with the items where f returns true. Returns {@link HashMap}.
         */
        public MapThreader<K, V> filter(BiFunction<K, V, Boolean> f) {
            Map<K, V> newMap = new HashMap<>();

            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (f.apply(entry.getKey(), entry.getValue())) {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            }

            map = newMap;
            return this;
        }

        /**
         * @param f    The function to apply to each value in collection. Arguments are the key and original value.
         * @param <NV> The new list type.
         * @return Threader with map, where f has been applied to each value in map. Returns {@link HashMap}.
         */
        public <NV> MapThreader<K, NV> mapv(BiFunction<K, V, NV> f) {
            Map<K, NV> newMap = new HashMap<>();

            map.forEach((key, value) -> newMap.put(key, f.apply(key, value)));

            return new MapThreader<>(newMap);
        }

        /**
         * @param f    The function to apply to each key in collection. Arguments are the original key and value.
         * @param <NK> The new key type.
         * @return Threader with map, where f has been applied to each key in map. Returns {@link HashMap}.
         */
        public <NK> MapThreader<NK, V> mapk(BiFunction<K, V, NK> f) {
            Map<NK, V> newMap = new HashMap<>();

            map.forEach((key, value) -> newMap.put(f.apply(key, value), value));

            return new MapThreader<>(newMap);
        }

        /**
         * Maps kf to each key and vf to each value. Modifications of k/v are independent of each other but executed at the same time.
         *
         * @param kf   The function to apply to each key in collection. Argument is the original key.
         * @param vf   The function to apply to each value in collection. Argument is the original value.
         * @param <NK> The new key type.
         * @param <NV> The new value type.
         * @return Threader with map, where kf has been applied to each key and vf to each value. Returns {@link HashMap}.
         */
        public <NK, NV> MapThreader<NK, NV> mapkv(Function<K, NK> kf, Function<V, NV> vf) {
            Map<NK, NV> newMap = new HashMap<>();

            map.forEach((key, value) -> newMap.put(kf.apply(key), vf.apply(value)));

            return new MapThreader<>(newMap);
        }

        /**
         * Performs consumer for every key and value.
         *
         * @param consumer The consumer.
         */
        public void forEach(BiConsumer<K, V> consumer) {
            map.forEach(consumer::accept);
        }


        /**
         * Maps all keys to values.
         *
         * @return Threader with map, where every value is the key to each key. Returns {@link HashMap}.
         */
        public MapThreader<V, K> inverse() {
            Map<V, K> newMap = new HashMap<>();

            map.forEach((k, v) -> newMap.put(v, k));

            return new MapThreader<>(newMap);
        }
    }

    /**
     * @param coll The collection.
     * @param <T>  The type.
     * @return A random item from coll.
     */
    public static <T> T random(List<T> coll) {
        return coll.get(ThreadLocalRandom.current().nextInt(coll.size()));
    }

    /**
     * @param coll The collection.
     * @param n    The amount of random items.
     * @param <T>  The type.
     * @return n random items from coll.
     */
    public static <T> List<T> random(List<T> coll, int n) {
        List<T> items = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            items.add(random(coll));
        }

        return items;
    }

    /**
     * @param start The start. Inclusive.
     * @param end   The end. Exclusive.
     * @param step  The increment between values.
     * @return List with all ints from start to end with increment step.
     */
    public static List<Integer> range(int start, int end, int step) {
        List<Integer> items = new ArrayList<>();

        for (int i = start; i < end; i += step) {
            items.add(i);
        }

        return items;
    }

    /**
     * @param start The start. Inclusive.
     * @param end   The end. Exclusive.
     * @return List with all ints from start to end with increment 1.
     */
    public static List<Integer> range(int start, int end) {
        return range(start, end, 1);
    }

    /**
     * @param end The end. Exclusive.
     * @return List with all ints from 0 to end with increment 1.
     */
    public static List<Integer> range(int end) {
        return range(0, end, 1);
    }
}