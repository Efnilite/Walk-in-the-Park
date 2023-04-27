package dev.efnilite.ip.leaderboard;

import dev.efnilite.vilib.util.Time;

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

    /**
     * @param old The old score format.
     * @return The new score format.
     */
    public static String parseV1Score(String old) {
        if (old.contains(":")) {
            return old;
        }

        double totalSec = 0; // total duration in ms

        for (String part : old.trim().split(" ")) {
            if (part.contains("h")) {
                totalSec += Integer.parseInt(part.replace("h", "")) * Time.SECONDS_PER_HOUR;
            } else if (part.contains("m")) {
                totalSec += Integer.parseInt(part.replace("m", "")) * Time.SECONDS_PER_MINUTE;
            } else if (part.contains("s")) {
                totalSec += Double.parseDouble(part.replace("s", ""));
            }
        }

        return timeFromMillis((int) (totalSec * 1000));
    }

    /**
     * @param millis The duration in millis.
     * @return The formatted time.
     */
    public static String timeFromMillis(int millis) {
        int m = millis / (60 * 1000);
        millis = millis - (m * 60 * 1000);

        int s = millis / 1000;
        millis = millis - (s * 1000);

        int ms = millis;

        return "%s:%s:%s".formatted(padLeft(Integer.toString(m), (m < 10) ? 1 : 0),
                padLeft(Integer.toString(s), (s < 10) ? 1 : 0),
                padLeft(Integer.toString(ms), (ms < 100) ? 2 : 0));
    }

    private static String padLeft(String s, int extraZeroes) {
        return extraZeroes > 0 ? String.format("%" + (extraZeroes + 1) + "s", s).replace(" ", "0") : s;
    }

    /**
     * @return This score's time in millis.
     */
    public int toMillis() {
        String[] split = time.split(":");

        int m = Integer.parseInt(split[0]);
        int s = Integer.parseInt(split[1]);
        int ms = Integer.parseInt(split[2]);

        return m * 60 * 1000 + s * 1000 + ms;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s%s%s%s%s", name, SPLITTER, time, SPLITTER, difficulty, SPLITTER, score);
    }
}