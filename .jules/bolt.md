# Bolt's Journal

## 2024-10-24 - Optimization Pattern: Fast Path for Bidi Checks
**Learning:** Adding a fast path check (e.g., `c < 0x0590`) in `TextBidi.couldAffectRtl` significantly improves performance for ASCII/Latin text insertion by skipping complex Bidi character checks.
**Action:** Always look for common-case fast paths in loop-heavy logic, especially for text processing where ASCII is dominant.

## 2024-10-24 - Environment Constraint: Gradle Toolchains
**Learning:** The build configuration enforces Java 17 compatibility using Gradle toolchains, which conflicts with the environment's Java 21 installation, causing build failures.
**Action:** When verifying changes in restricted environments, temporary build configuration adjustments (bumping toolchain version) may be necessary, but must be reverted before submission.
