# Kotlin, Android, and Multiplatform

- Use precise types, immutable values, and explicit nullability; model closed
  states with sealed hierarchies/enums. Avoid unchecked casts, unnecessary `!!`,
  and scope-function chains that obscure flow.
- Keep platform APIs out of common source sets. Use small interfaces or
  `expect`/`actual` only when shared code needs platform behavior.
- Own structured concurrency for the work's lifetime; keep blocking I/O off UI/event
  loops and inject dispatchers for substitution. Never bypass lifecycle cancellation
  with unowned scopes.
- Prefer `runCatching` where clearer and suitable, such as non-suspending fallbacks;
  retain `try`/`catch` for specific exceptions, `finally` cleanup, or rethrowing. Rethrow
  `CancellationException` in broad catches with either approach. Handle expected
  failures explicitly; release resources on cancellation and define overlapping
  requests' effects on state.
- Expose read-only state with owner-controlled mutations and atomic updates/
  synchronization; distinguish loading, empty, success, and failure where needed.
- Respect Android lifecycle, process recreation, permissions, and background limits.
  Persist only necessary recreation state; never retain activities/views in longer-lived owners.
- Reuse Gradle wrapper, catalog, and conventions; align plugin, Kotlin, runtime, SDK,
  and compiler compatibility. Scope experimental opt-ins narrowly; verify migrations
  on supported targets.
- Test portable behavior in common tests and platform contracts in platform tests.
