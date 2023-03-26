package dev.efnilite.ip.util;

import java.util.ArrayList;
import java.util.List;
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
     * @param <T>  The original list type.
     * @param <N>  The new list type.
     * @return coll where f has been applied to each item. Retains order.
     */
    public static <T, N> List<N> map(Function<T, N> f, List<T> coll) {
        List<N> newColl = new ArrayList<>();

        for (T item : coll) {
            newColl.add(f.apply(item));
        }

        return newColl;
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
     * @param end The end. Exclusive.
     * @param step The increment between values.
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
     * @param end The end. Exclusive.
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