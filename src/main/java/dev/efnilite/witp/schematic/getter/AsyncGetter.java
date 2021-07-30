package dev.efnilite.witp.schematic.getter;

/**
 * An interface related to all asynchronous data getters,
 * which don't return as it should, since it's on a
 * different Thread.
 *
 * Taken from: Efnilite/Redaktor
 *
 * @param   <T>
 *          The return value when the asynchronous
 *          data collection is done.
 */
public interface AsyncGetter<T> {

    /**
     * When the async retrieving is done this method gets called.
     *
     * @param   value
     *          What the async retrieving gathered.
     */
    void asyncDone(T value);
}