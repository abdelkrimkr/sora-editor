## 2025-02-19 - ContentLine Insertion Optimization
**Learning:** `ContentLine` insertion was using a character-by-character loop with `s.charAt(i)` to copy text and check for Bidi properties simultaneously. Splitting this into two passes—first a bulk copy using `TextUtils.getChars` (which uses `System.arraycopy` internally), followed by a scan of the local char array—resulted in a ~37% performance improvement for large insertions.
**Action:** When optimizing text buffer operations, prefer bulk copies (`System.arraycopy` or `getChars`) followed by local array iteration over interface-dispatch-heavy loops (`CharSequence.charAt`).

## 2025-02-19 - CachedIndexer Eviction Policy & Allocation
**Learning:** `CachedIndexer` had an inconsistent eviction policy: items were added to the end (treating 0 as LRU) but accessed items were swapped to 0 (treating 0 as MRU), causing recently accessed items to be evicted immediately upon new insertions. Also, `afterDelete` created temporary `ArrayList`s to collect items for removal.
**Action:** Ensure LRU caches maintain consistent directionality (e.g., add to End, swap accessed to End, remove from Start). Use `Iterator.remove()` for conditional removal from lists to avoid allocation overhead.
