package io.github.rosemoe.sora.text;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class CachedIndexerTest {

    @Test
    public void testLruEvictionPolicy() throws Exception {
        // Create Content with enough lines (need > 5 * 50 lines to fill cache with default threshold 50)
        // Let's go big.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("line ").append(i).append('\n');
        }
        Content content = new Content(sb.toString(), false);
        CachedIndexer indexer = (CachedIndexer) content.getIndexer();

        // Reflection to access cachedPositions
        Field cacheField = CachedIndexer.class.getDeclaredField("cachedPositions");
        cacheField.setAccessible(true);
        List<CharPosition> cache = (List<CharPosition>) cacheField.get(indexer);

        // Set max cache size to a small number for testing
        Field maxCacheCountField = CachedIndexer.class.getDeclaredField("maxCacheCount");
        maxCacheCountField.setAccessible(true);
        maxCacheCountField.set(indexer, 5); // Size 5

        // thresholdLine is final 50. We can't change it easily via reflection if inlined.
        // So we use stride > 50.
        int stride = 60;

        // Fill cache with 5 items.
        // We start from line 60.
        // 1. 60 (Dist 60 > 50) -> Cached.
        // 2. 120 (Dist 60 > 50 from 60) -> Cached.
        // ...
        for (int i = 1; i <= 5; i++) {
             indexer.getCharPosition(i * stride, 0);
        }

        assertEquals("Cache should be full", 5, cache.size());

        // Cache should contain roughly: 60, 120, 180, 240, 300.
        // Order: [60, 120, 180, 240, 300] (because we fixed push to not remove 0 if not full, and findNearest moves to end)
        // Actually, 'push' adds to end.

        // Identify a victim. Let's pick the middle one (180, index 2).
        CharPosition itemToAccess = cache.get(2);
        int targetLine = itemToAccess.line;

        // Access it to make it MRU
        indexer.getCharPosition(targetLine, 0);

        // With FIX: 180 moves to end.
        // Cache: [60, 120, 240, 300, 180]

        // Add one more item (360)
        indexer.getCharPosition(360, 0);

        // Push adds 360 to end. Removes 0 (which is 60).
        // Cache: [120, 240, 300, 180, 360]

        // 180 should still be there.

        boolean containsTarget = false;
        for (CharPosition p : cache) {
            if (p.line == targetLine) {
                containsTarget = true;
                break;
            }
        }

        assertTrue("MRU item (Line " + targetLine + ") should be preserved after eviction", containsTarget);

        // Check that line 60 (the original index 0) is gone (LRU)
        boolean containsLRU = false;
        for (CharPosition p : cache) {
            if (p.line == 60) {
                containsLRU = true;
                break;
            }
        }
        assertFalse("LRU item (Line 60) should be evicted", containsLRU);
    }

    @Test
    public void testAfterDeleteCorrectness() throws Exception {
        // Setup content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("line").append(i).append("\n");
        }
        Content content = new Content(sb.toString(), false);
        CachedIndexer indexer = (CachedIndexer) content.getIndexer();

        Field cacheField = CachedIndexer.class.getDeclaredField("cachedPositions");
        cacheField.setAccessible(true);
        List<CharPosition> cache = (List<CharPosition>) cacheField.get(indexer);

        // thresholdLine is 50.
        int stride = 60;

        // Fill cache
        indexer.getCharPosition(stride * 1, 0); // 60
        indexer.getCharPosition(stride * 2, 0); // 120
        indexer.getCharPosition(stride * 3, 0); // 180 - To be deleted/affected
        indexer.getCharPosition(stride * 4, 0); // 240

        // Delete lines around 180.
        // Delete 175 to 185.
        content.delete(175, 0, 185, 0);

        // Line 180 is inside deletion range (175..185). Should be removed.

        for (CharPosition p : cache) {
            assertFalse("Deleted line 180 should not be in cache", p.line == 180);
        }

        // Line 240 should be shifted.
        // 175..185 is 10 lines.
        // 240 -> 230.

        boolean foundShifted = false;
        for (CharPosition p : cache) {
            if (p.line == 230) {
                foundShifted = true;
            }
        }
        assertTrue("Line 240 should be shifted to 230", foundShifted);
    }
}
