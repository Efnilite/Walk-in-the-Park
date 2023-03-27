package dev.efnilite.ip.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Colls {

    /**
     * @param f    The function to apply.
     * @param coll The collection.
     * @param <T>  The type.
     * @return All items in coll where f returns true.
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
     * @return coll where f has been applied to each item. Retains order.
     */
    public static <OV, NV> List<NV> mapv(Function<OV, NV> f, List<OV> coll) {
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
     * @return map where f has been applied to each value. Returns {@link HashMap}.
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
     * @return map where f has been applied to each key. Returns {@link HashMap}.
     */
    public static <OK, NK, V> Map<NK, V> mapk(BiFunction<OK, V, NK> f, Map<OK, V> map) {
        Map<NK, V> newMap = new HashMap<>();

        map.forEach((key, value) -> newMap.put(f.apply(key, value), value));

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
    public static <T> T reduce(List<T> coll, T startValue, BiFunction<T, T, T> f) {
        T item = startValue;

        for (int i = 1; i < coll.size() - 1; i++) {
            T newItem = coll.get(i);

            item = f.apply(item, newItem);
        }

        return item;
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
    public static <T> T reduce(List<T> coll, BiFunction<T, T, T> f) {
        T item = coll.get(0);

        for (int i = 1; i < coll.size() - 1; i++) {
            T newItem = coll.get(i);

            item = f.apply(item, newItem);
        }

        return item;
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