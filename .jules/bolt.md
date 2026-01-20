## 2024-05-22 - [Optimization Pattern: Bidi Fast Path]
**Learning:** `TextBidi.couldAffectRtl` was performing multiple checks for every character, including ASCII. Adding a fast path check (`c < 0x0590`) significantly improves performance for ASCII/Latin text insertion by skipping complex Bidi character checks. This provided a ~2x speedup in synthetic benchmarks for ASCII text.
**Action:** Look for similar "hot loop" character checks where a simple range check can skip expensive logic for the common case (ASCII).
