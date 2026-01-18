package io.github.rosemoe.sora.text.bidi;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextBidiBenchmarkTest {

    @Test
    public void testCouldAffectRtlCorrectness() {
        // ASCII
        assertFalse(TextBidi.couldAffectRtl('a'));
        assertFalse(TextBidi.couldAffectRtl('z'));
        assertFalse(TextBidi.couldAffectRtl('0'));
        assertFalse(TextBidi.couldAffectRtl(' '));
        assertFalse(TextBidi.couldAffectRtl('\n'));

        // Hebrew (RTL)
        assertTrue(TextBidi.couldAffectRtl('\u0590'));
        assertTrue(TextBidi.couldAffectRtl('\u05D0'));

        // Arabic (RTL)
        assertTrue(TextBidi.couldAffectRtl('\u0600'));

        // Bidi format chars
        assertTrue(TextBidi.couldAffectRtl('\u200E')); // LRM
        assertTrue(TextBidi.couldAffectRtl('\u200F')); // RLM
        assertTrue(TextBidi.couldAffectRtl('\u202A')); // LRE
        assertTrue(TextBidi.couldAffectRtl('\u202E')); // RLO

        // Surrogates (might need bidi if they form RTL chars)
        assertTrue(TextBidi.couldAffectRtl('\uD800'));
        assertTrue(TextBidi.couldAffectRtl('\uDFFF'));

        // Presentation forms
        assertTrue(TextBidi.couldAffectRtl('\uFB1D'));
        assertTrue(TextBidi.couldAffectRtl('\uFE70'));
    }

    @Test
    public void benchmarkCouldAffectRtl() {
        // Prepare a large buffer of ASCII characters
        int size = 1000000;
        char[] buffer = new char[size];
        for (int i = 0; i < size; i++) {
            buffer[i] = (char) ('a' + (i % 26));
        }

        // Warmup
        for (int i = 0; i < 20; i++) {
            runCheck(buffer);
        }

        // Measure
        long startTime = System.nanoTime();
        int iterations = 200;
        for (int i = 0; i < iterations; i++) {
            runCheck(buffer);
        }
        long endTime = System.nanoTime();

        long totalTime = endTime - startTime;
        System.out.println("Benchmark time: " + (totalTime / 1_000_000.0) + " ms");
        System.out.println("Time per char: " + (totalTime / (double)(size * iterations)) + " ns");
    }

    private int runCheck(char[] buffer) {
        int count = 0;
        for (char c : buffer) {
            if (TextBidi.couldAffectRtl(c)) {
                count++;
            }
        }
        return count;
    }
}
