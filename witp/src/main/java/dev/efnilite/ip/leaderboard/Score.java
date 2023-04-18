package dev.efnilite.ip.leaderboard;

import dev.efnilite.vilib.util.Time;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a record, used to keep track of the score a player may achieve.
 *
 * @param name       The name of the player
 * @param score      The score achieved
 * @param time       The time it took to achieve this score
 * @param difficulty The difficulty of this run
 */
public record Score(String name, String time, String difficulty, int score) {

    /**
     * The time format for scores.
     */
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("mm:ss:SSS");

    /**
     * The character used for splitting in strings
     */
    private static final String SPLITTER = ",";

    /**
     * Gets a {@link Score} instance from a string
     *
     * @param string The string
     * @return a {@link Score} instance based off the provided string
     */
    public static Score fromString(String string) {
        String[] parts = string.split(SPLITTER);

        return new Score(parts[0], parseV1Score(parts[1]), parts[2], Integer.parseInt(parts[3]));
    }

    @Override
    public String toString() {
        return String.format("%s%s%s%s%s%s%s", name, SPLITTER, time, SPLITTER, difficulty, SPLITTER, score);
    }

    private static String parseV1Score(String old) {
        if (old.contains(":")) {
            return old;
        }

        long totalMs = 0; // total duration in ms

        for (String part : old.trim().split(" ")) {
            if (part.contains("h")) {
                totalMs += Time.toMillis((long) Integer.parseInt(part.replace("h", "")) * Time.SECONDS_PER_HOUR);
            } else if (part.contains("m")) {
                totalMs += Time.toMillis((long) Integer.parseInt(part.replace("m", "")) * Time.SECONDS_PER_MINUTE);
            } else if (part.contains("s")) {
                totalMs += Double.parseDouble(part.replace("s", "")) * 1000;
            }
        }

        return TIME_FORMAT.format(new Date(totalMs));
    }
}