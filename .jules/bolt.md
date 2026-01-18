## 2026-01-18 - TextBidi Optimization
**Learning:** `TextBidi.couldAffectRtl` is a hot path for text insertion. Adding a fast path check (`c < 0x0590`) for ASCII/Latin text improved performance by ~3-4x (from ~3.5ns to ~0.5-1.0ns per char) on microbenchmarks.
**Action:** Identify character processing loops and implement fast paths for common scripts (like ASCII).
