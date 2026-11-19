# Git operations

- Use concise, descriptive subjects in imperative mood; prefer lowercase
  descriptions without trailing periods, subject to the chosen convention.
- For Conventional Commits, use `<type>(<optional-scope>): <imperative description>`;
  omit parentheses when no meaningful scope applies. Common types: `feat`, `fix`,
  `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`, `style`, `revert`.
  Examples: `feat(search): add game filters`, `fix(api): handle empty responses`,
  `refactor(ui): simplify navigation`.
- Represent types and breaking-change status accurately. For incompatible changes
  under Conventional Commits, put `!` immediately before `:` (e.g., `feat(api)!:`)
  or add a `BREAKING CHANGE: <description>` footer separated by a blank line;
  either or both may be used. Describe the incompatibility.
