package dev.efnilite.ip.player;

/**
 * Represents a record, used to keep track of the score a player may achieve.
 *
 * @param name The name of the player
 * @param score The score achieved
 * @param time The time it took to achieve this score
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

        return new Score(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
    }

    @Override
    public String toString() {
        return String.format("%s%s%s%s%s%s%s", name, SPLITTER, time, SPLITTER, difficulty, SPLITTER, score);
    }
}