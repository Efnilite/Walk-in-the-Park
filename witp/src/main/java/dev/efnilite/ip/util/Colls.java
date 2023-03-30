package dev.efnilite.ip.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Colls {

    /**
     * @param f    The function to apply.
     * @param coll The collection.
     * @param <T>  The type.
     * @return List with all items in coll where f returns true.
     */
    public static <T> List<T> filter(Function<T, Boolean> f, List<T> coll) {
        List<T> newColl = new ArrayList<>();

        for (T item : coll) {
            if (f.apply(item)) {
                newColl.add(item);
            }
        }

        return newColl;
    }

    /**
     * @param f    The function to apply to each item in collection.
     * @param coll The collection.
     * @param <OV> The original list type.
     * @param <NV> The new list type.
     * @return List where f has been applied to each item in coll. Retains order.
     */
    public static <OV, NV> List<NV> map(Function<OV, NV> f, List<OV> coll) {
        List<NV> newColl = new ArrayList<>();

        for (OV item : coll) {
            newColl.add(f.apply(item));
        }

        return newColl;
    }

    /**
     * @param f    The function to apply to each value in collection. Arguments are the key and original value.
     * @param map  The map.
     * @param <K>  The key type.
     * @param <OV> The original list type.
     * @param <NV> The new list type.
     * @return Map where f has been applied to each value in map. Returns {@link HashMap}.
     */
    public static <K, OV, NV> Map<K, NV> mapv(BiFunction<K, OV, NV> f, Map<K, OV> map) {
        Map<K, NV> newMap = new HashMap<>();

        map.forEach((key, value) -> newMap.put(key, f.apply(key, value)));

        return newMap;
    }

    /**
     * @param f    The function to apply to each key in collection. Arguments are the original key and value.
     * @param map  The map.
     * @param <OK> The original key type.
     * @param <NK> The new key type.
     * @param <V>  The value type.
     * @return Map where f has been applied to each key in map. Returns {@link HashMap}.
     */
    public static <OK, NK, V> Map<NK, V> mapk(BiFunction<OK, V, NK> f, Map<OK, V> map) {
        Map<NK, V> newMap = new HashMap<>();

        map.forEach((key, value) -> newMap.put(f.apply(key, value), value));

        return newMap;
    }

    /**
     * Maps kf to each key and vf to each value. Modifications of k/v are independent of each other but executed at the same time.
     *
     * @param kf   The function to apply to each key in collection. Argument is the original key.
     * @param vf   The function to apply to each value in collection. Argument is the original value.
     * @param map  The map.
     * @param <OK> The original key type.
     * @param <NK> The new key type.
     * @param <OV> The original value type.
     * @param <NV> The new value type.
     * @return Map where kf has been applied to each key and vf to each value. Returns {@link HashMap}.
     */
    public static <OK, NK, OV, NV> Map<NK, NV> mapkv(Function<OK, NK> kf, Function<OV, NV> vf, Map<OK, OV> map) {
        Map<NK, NV> newMap = new HashMap<>();

        map.forEach((key, value) -> newMap.put(kf.apply(key), vf.apply(value)));

        return newMap;
    }

    /**
     * Maps all keys to values.
     *
     * @param map The map.
     * @param <K> The key type.
     * @param <V> The value type.
     * @return Map where every value is the key to each key. Returns {@link HashMap}.
     */
    public static <K, V> Map<V, K> inverse(Map<K, V> map) {
        Map<V, K> newMap = new HashMap<>();

        map.forEach((k, v) -> newMap.put(v, k));

        return newMap;
    }

    /**
     * Performs function f on the starting value and first item in coll, then
     * the result of that and the second item in coll, etc.
     *
     * @param coll       The collection.
     * @param startValue The starting value.
     * @param f          The function to apply to items.
     * @param <T>        The type.
     * @return A single value of the same type.
     */
    public static <T> T reduce(Collection<T> coll, T startValue, BiFunction<T, T, T> f) {
        T old = startValue;

        for (T new_ : coll) {
            old = f.apply(old, new_);
        }

        return old;
    }

    /**
     * Performs function f on the first and second item in coll, then
     * the result of that and the third item in coll, etc.
     *
     * @param coll The collection.
     * @param f    The function to apply to items.
     * @param <T>  The type.
     * @return A single value of the same type.
     */
    public static <T> T reduce(Collection<T> coll, BiFunction<T, T, T> f) {
        T old = null;

        for (T new_ : coll) {
            if (old == null) {
                old = new_;
                continue;
            }

            old = f.apply(old, new_);
        }

        return old;
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