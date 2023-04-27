package dev.efnilite.ip;

import dev.efnilite.ip.leaderboard.Score;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreTest {

    @Test
    public void testFromString() {
        Score score = Score.fromString("Player1,01:23:456,hard,100");
        assertEquals("Player1", score.name());
        assertEquals("01:23:456", score.time());
        assertEquals("hard", score.difficulty());
        assertEquals(100, score.score());
    }

    @Test
    public void testToString() {
        Score score = new Score("Player2", "02:34:567", "easy", 50);
        assertEquals("Player2,02:34:567,easy,50", score.toString());
    }

    @Test
    public void testParseV1Score() {
        assertEquals("83:45:678", Score.parseV1Score("1h 23m 45.678s"));
        assertEquals("03:45:000", Score.parseV1Score("3m 45s"));
        assertEquals("00:05:040", Score.parseV1Score("5.04s"));
        assertEquals("00:05:009", Score.parseV1Score("5.009s"));
    }
}