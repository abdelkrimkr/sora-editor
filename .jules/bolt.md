## 2025-02-19 - ContentLine Insertion Optimization
**Learning:** `ContentLine` insertion was using a character-by-character loop with `s.charAt(i)` to copy text and check for Bidi properties simultaneously. Splitting this into two passes—first a bulk copy using `TextUtils.getChars` (which uses `System.arraycopy` internally), followed by a scan of the local char array—resulted in a ~37% performance improvement for large insertions.
**Action:** When optimizing text buffer operations, prefer bulk copies (`System.arraycopy` or `getChars`) followed by local array iteration over interface-dispatch-heavy loops (`CharSequence.charAt`).

## 2025-02-19 - CachedIndexer Eviction Policy Fix
**Learning:** The `CachedIndexer` used an "anti-cache" eviction policy: accessed items were moved to index 0 (assumed MRU), but new items were added to the end, and the cache was full, index 0 was evicted. This effectively evicted the most recently accessed item.
**Action:** Ensure LRU caches correctly align "move to MRU" logic (e.g., move to end) with "evict LRU" logic (e.g., remove from start).
