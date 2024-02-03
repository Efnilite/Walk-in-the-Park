package dev.efnilite.ip;

import dev.efnilite.ip.leaderboard.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {

    private Score score;

    @BeforeEach
    void setUp() {
        score = new Score("player", "00:00:00", "1.0", 100);
    }

    @Test
    void testFromString() {
        var result = Score.fromString("player,00:00:00,1.0,100");

        assertEquals(score, result);
    }

    @Test
    void testGetTimeMillis() {
        int result = score.getTimeMillis();

        assertEquals(0, result);
    }

    @Test
    void testToString() {
        var result = score.toString();

        assertEquals("player,00:00:00,1.0,100", result);
    }
}