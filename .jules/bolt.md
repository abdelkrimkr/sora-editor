## 2026-01-17 - Fast Path Optimization in Text Scanning
**Learning:** `TextBidi.couldAffectRtl` is a hot path called for every character during text insertion and processing. It involves a chain of 8 comparisons. Adding a simple fast path check `c < 0x0590` (covering ASCII/Latin) yielded a ~6.7x speedup (3.35ns -> 0.49ns per char) on microbenchmarks.
**Action:** Always check for fast paths in character scanning loops, especially for ASCII in code editors.
