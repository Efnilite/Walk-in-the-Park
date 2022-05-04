package dev.efnilite.ip.player.data;

/**
 * Represents a record, used to keep track of the score a player may achieve.
 *
 * @param   name
 *          The name of the player
 *
 * @param   score
 *          The score achieved
 *
 * @param   time
 *          The time it took to achieve this score
 *
 * @param   difficulty
 *          The difficulty of this run
 */
public record Score(String name, int score, String time, String difficulty) {

}