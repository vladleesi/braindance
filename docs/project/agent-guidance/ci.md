# CI workflows

- Distinguish introduced, pre-existing, and infrastructure failures. Avoid redundant
  polling, unchanged checks, unbounded retries, and unrelated fixes.
- Pin supported toolchains and restore dependencies reproducibly. Key caches by
  relevant inputs; support misses. Set timeouts; cancel obsolete checks without
  interrupting releases requiring serialization.
- Preserve project GitHub Action and reusable-workflow pinning policy. Full commit
  SHAs identify immutable revisions; tags can move. Review source repositories,
  publishers, and changes before adoption or updates.
