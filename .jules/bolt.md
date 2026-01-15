## 2024-05-22 - Bulk Character Insertion Optimization
**Learning:** `CharSequence.charAt()` loops for text insertion are inefficient compared to bulk copy operations like `String.getChars()` or `System.arraycopy()`.
**Action:** Always check if a `CharSequence` is a `String`, `StringBuilder`, or implements `GetChars` to use bulk copy methods before falling back to `charAt` loops. Even with a subsequent pass for RTL checking, bulk copying + array iteration is often faster due to reduced method call overhead and better memory access patterns.
