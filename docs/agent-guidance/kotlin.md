# Kotlin, Android, and Multiplatform

- Prefer precise types, immutable values, and explicit nullability. Model closed
  states with sealed hierarchies or enums; avoid unchecked casts, unnecessary
  `!!`, and scope-function chains that obscure flow.
- Keep platform APIs out of common source sets. Use small interfaces or
  `expect`/`actual` only when shared code needs platform behavior. Verify
  dependency and API support for every affected target and source set.
- Use structured concurrency owned for the work's lifetime. Keep blocking I/O
  off UI/event-loop threads; inject dispatchers when substitution is needed.
  Never use unowned scopes to bypass lifecycle cancellation.
- Rethrow `CancellationException` in broad catches, including `runCatching`
  recovery. Handle expected failures explicitly; release resources on
  cancellation. Define overlapping requests' effects on state.
- Expose read-only state; keep mutations with its owner. Use atomic updates or
  synchronization for concurrency; distinguish loading, empty, success, and
  failure where callers or UI need it.
- Respect Android lifecycle, process recreation, permissions, and background
  limits. Persist only state that must survive recreation; never retain
  activities or views in longer-lived owners.
- Reuse the Gradle wrapper, dependency catalog, and conventions. Align plugin,
  Kotlin, runtime, SDK, and compiler compatibility. Scope experimental opt-ins
  narrowly; verify migrations on supported targets.
- Test portable behavior in common tests and platform contracts in platform
  tests. Compile affected source sets; one target's success does not establish
  others' compatibility.
