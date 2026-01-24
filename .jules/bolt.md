## 2025-02-19 - ContentLine Insertion Optimization
**Learning:** `ContentLine` insertion was using a character-by-character loop with `s.charAt(i)` to copy text and check for Bidi properties simultaneously. Splitting this into two passes—first a bulk copy using `TextUtils.getChars` (which uses `System.arraycopy` internally), followed by a scan of the local char array—resulted in a ~37% performance improvement for large insertions.
**Action:** When optimizing text buffer operations, prefer bulk copies (`System.arraycopy` or `getChars`) followed by local array iteration over interface-dispatch-heavy loops (`CharSequence.charAt`).

## 2025-10-21 - Manual Cache Eviction Policy
**Learning:** `CachedIndexer`'s manual LRU implementation was broken: `findNearest` promoted items to index 0 (MRU), but `push` removed index 0 (MRU) when full, causing the most frequently used items to be evicted immediately upon new insertions. This negated the benefit of the cache.
**Action:** Always verify the eviction policy of manual cache implementations with a unit test that explicitly checks MRU preservation.
