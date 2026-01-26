## 2025-02-19 - ContentLine Insertion Optimization
**Learning:** `ContentLine` insertion was using a character-by-character loop with `s.charAt(i)` to copy text and check for Bidi properties simultaneously. Splitting this into two passes—first a bulk copy using `TextUtils.getChars` (which uses `System.arraycopy` internally), followed by a scan of the local char array—resulted in a ~37% performance improvement for large insertions.
**Action:** When optimizing text buffer operations, prefer bulk copies (`System.arraycopy` or `getChars`) followed by local array iteration over interface-dispatch-heavy loops (`CharSequence.charAt`).

## 2025-02-19 - CachedIndexer MRU Eviction Bug
**Learning:** `CachedIndexer` implemented an incorrect LRU policy. Accessing a cached item moved it to index 0 using `Collections.swap`. Since the eviction policy (`push`) removes from index 0, accessing an item inadvertently marked it for immediate eviction. This effectively implemented an anti-LRU policy where frequently accessed items were evicted first.
**Action:** When implementing LRU caches with `ArrayList`, avoid `swap` to the eviction end. Instead, use `remove(index)` followed by `add(item)` to correctly move the accessed item to the "safe" end (MRU), ensuring strictly correct LRU behavior.
