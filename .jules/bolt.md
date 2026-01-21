## 2025-02-19 - TextBidi Optimization
**Learning:** `ContentLine` insertion was using a character-by-character loop with `s.charAt(i)` to copy text and check for Bidi properties simultaneously. Splitting this into two passes—first a bulk copy using `TextUtils.getChars` (which uses `System.arraycopy` internally), followed by a scan of the local char array—resulted in a ~37% performance improvement for large insertions.
**Action:** When optimizing text buffer operations, prefer bulk copies (`System.arraycopy` or `getChars`) followed by local array iteration over interface-dispatch-heavy loops (`CharSequence.charAt`).

## 2025-10-26 - TextBidi Fast Path
**Learning:** `TextBidi.couldAffectRtl` checks many ranges and conditions for every character. Since most code is ASCII/Latin ( < 0x0590), adding a fast path check `if (c < 0x0590) return false;` at the beginning avoids multiple comparisons for the vast majority of characters. Benchmarks showed a ~1.84x speedup for predominantly ASCII text.
**Action:** Always check for common fast paths (like ASCII checks) before entering complex validation logic in character processing loops.
