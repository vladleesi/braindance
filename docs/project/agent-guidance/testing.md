# Testing and validation

- Focus checks on changed behavior; broaden for shared APIs, packaging, or
  platform changes. Docs-only edits normally need link, example, and diff review
  unless project policy requires more.
- Test observable success, boundary, and failure contracts, not incidental structure.
  Reproduce bugs and add regression tests when practical; cover shared causes and
  affected integrations beyond the symptom.
- Mock external boundaries where appropriate; retain integration coverage for
  serialization, persistence, framework behavior, and contracts mocks can hide.
- Use interaction, visual, browser, or device checks where static tests cannot
  establish UI correctness; cover affected states and platforms.
