# CI workflows

- Distinguish failures introduced by changes from pre-existing or infrastructure
  failures. Avoid redundant polling, repeated unchanged checks, unbounded retries,
  and unrelated fixes.
- Preserve project GitHub Action and reusable-workflow pinning policy. Full commit
  SHAs identify immutable revisions; tags can move. Review source repositories,
  publishers, and changes before adoption or updates.
