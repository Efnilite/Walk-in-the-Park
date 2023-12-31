package dev.efnilite.ip;

import dev.efnilite.ip.generator.JumpDirector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JumpDirectorTest {

    private BoundingBox bb;

    @BeforeEach
    void setUp() {
        var corner1 = new Vector(0, 0, 0);
        var corner2 = new Vector(100, 100, 100);

        bb = BoundingBox.of(corner1, corner2);
    }

    @Test
    void testProgress() {
        var point = new Vector(50, 50, 50);
        var director = new JumpDirector(bb, point);
        var progress = director.getProgress();

        assertEquals(progress[0][0], 0.5);
        assertEquals(progress[1][0], 0.5);
        assertEquals(progress[2][0], 0.5);
    }

    @Test
    void testRecommendedHeading() {
        var point = new Vector(95, 50, 50);
        var director = new JumpDirector(bb, point);
        var heading = director.getRecommendedHeading();

        assertEquals(heading.getX(), -1);
        assertEquals(heading.getY(), 0);
        assertEquals(heading.getZ(), 0);
    }
}
