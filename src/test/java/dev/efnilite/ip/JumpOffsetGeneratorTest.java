package dev.efnilite.ip;

import dev.efnilite.ip.generator.JumpOffsetGenerator;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class JumpOffsetGeneratorTest {

    @Test
    public void testOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> new JumpOffsetGenerator(3, 0));
        assertThrows(IllegalArgumentException.class, () -> new JumpOffsetGenerator(-3, 0));
    }

    @Test
    public void testPurity() {
        var generator1 = new JumpOffsetGenerator(0, 2);
        var offset1 = generator1.getRandomOffset(0, 1, new Random(1));

        var generator2 = new JumpOffsetGenerator(0, 2);
        var offset2 = generator2.getRandomOffset(0, 1, new Random(1));

        assertEquals(offset1, offset2);
    }

    @Test
    public void testSequence() {
        var generator = new JumpOffsetGenerator(0, 1);

        var first = new int[10];

        var random1 = new Random(1);
        for (int i = 0; i < 10; i++) {
            first[i] = generator.getRandomOffset(0, 1, random1);
        }

        assertArrayEquals(first, new int[] {1, 0, -1, 0, 2, -1, 2, -3, 2, -3});
    }
}
