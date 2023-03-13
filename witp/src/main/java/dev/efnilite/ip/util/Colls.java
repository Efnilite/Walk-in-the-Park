package dev.efnilite.ip.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Colls {

    /**
     * Applies function f to each item in coll.
     *
     * @param f The function to apply to each item in collection.
     * @param coll The collection.
     * @return coll where each function f has been applied to each item.
     * @param <T> The original list type.
     * @param <N> The new list type.
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
     * @param coll The collection.
     * @param startValue The starting value.
     * @param f The function to apply to items.
     * @return A single value of the same type.
     * @param <T> The type.
     */
    public static <T> T reduce(List<T> coll, T startValue, BiFunction<T, T, T> f) {
        T item = startValue;

        for (int i = 1; i < coll.size() - 1; i++) {
            T newItem = coll.get(i);

            item = f.apply(item, newItem);
        }

        return item;
    }

    public static <T> T reduce(List<T> coll, BiFunction<T, T, T> f) {
        T item = coll.get(0);

        for (int i = 1; i < coll.size() - 1; i++) {
            T newItem = coll.get(i);

            item = f.apply(item, newItem);
        }

        return item;
    }

}