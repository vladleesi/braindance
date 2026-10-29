# Python

- Use the project's supported interpreter and isolated environment. Declare
  runtime and dev dependencies; avoid import-time network, filesystem, and
  config side effects.
- Annotate public boundaries and non-obvious data structures. Prefer standard
  collections and small explicit models. Validate external values at runtime;
  annotations do not validate data.
- Catch specific exceptions where possible; preserve causes with exception
  chaining. Propagate cancellation and process-termination signals.
- Manage files, locks, sessions, and temporary resources with context managers.
  Define iterator and async-task lifetimes; avoid mutable default arguments and
  hidden shared request state.
- Keep blocking work out of async handlers: use async-compatible dependencies or
  deliberate thread/process boundaries. Do not mark synchronous work `async`
  without a concurrency benefit.
- Invoke subprocesses with argument lists; handle expected exit codes
  explicitly. Specify persistent-format encodings; use filesystem path APIs.
  Never build shell commands from untrusted input.
- Reuse project formatters, linters, type checkers, and test runners. Justify
  and narrowly scope suppressions; test real imports and entry points for
  packaging or startup changes.
