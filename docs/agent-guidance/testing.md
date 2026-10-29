# Testing and validation

- Focus checks on changed behavior; broaden for shared APIs, packaging, or
  platform changes. Docs-only edits normally need link, example, and diff review
  unless project policy requires more.
- Test observable contracts with meaningful success, boundary, and failure
  cases; avoid restating implementation or locking down incidental structure.
- Reproduce bugs; add regression tests when practical. Cover shared causes and
  affected integration points beyond the immediate symptom.
- Keep tests deterministic and isolated from live services and user data. Use
  test credentials, temporary resources, controlled clocks, and explicit
  cleanup. Synchronize concurrency tests instead of guessing timing.
- Mock external boundaries where appropriate; retain integration tests for
  serialization, persistence, framework behavior, and contracts mocks can hide.
- Check affected cancellation, retry, authorization-failure, and data-loss
  paths. Verify migrations with representative disposable data.
- Use interaction, visual, browser, or device checks where static UI tests
  cannot establish correctness; cover affected states and platforms.
- Validate the distributable when build output or delivery changes. A dev server
  alone cannot establish production behavior or performance.
- Fix failures without weakening assertions or budgets just to pass.
