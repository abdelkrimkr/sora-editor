## 2025-02-18 - Failed Optimization: GetChars with Loop
**Learning:** Bulk copying text with `getChars` (System.arraycopy) followed by a separate loop for Bidi checks was ~4x slower than a single combined loop on HotSpot JVM (Java 21). This is likely due to the overhead of reading back the array just written (cache/memory bandwidth) versus keeping the character in a register during a single pass.
**Action:** Prefer single-pass loops for simultaneous copy-and-check operations unless the check can be skipped entirely or vectorized efficiently.
