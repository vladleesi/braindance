# Python

- Use the project's supported interpreter/isolated environment; declare runtime/dev
  dependencies. Avoid import-time network, filesystem, and config side effects.
- Annotate public boundaries and non-obvious structures; prefer standard collections
  and small explicit models.
- Catch specific exceptions where possible, preserve causes via chaining, and propagate
  cancellation and process-termination signals.
- Use context managers for files, locks, sessions, and temporary resources. Define
  iterator/async-task lifetimes; avoid mutable defaults and hidden shared request state.
- Keep blocking work out of async handlers; use async dependencies or deliberate
  thread/process boundaries. Do not mark synchronous work `async` without a
  concurrency benefit.
- Specify persistent-format encodings; use filesystem path APIs.
- Reuse project formatters, linters, type checkers, and test runners; justify and
  narrowly scope suppressions.
