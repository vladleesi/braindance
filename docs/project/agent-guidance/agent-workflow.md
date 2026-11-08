# Agent workflow

- Start with filename and bounded text searches (`rg --files`, `rg`). Trace
  affected flows through callers, config, and nearby tests; expand as evidence
  requires.
- Load only task-relevant guidance and documentation. Verify uncertain or
  version-sensitive APIs against primary documentation for the project's
  versions. Stop research when uncertainty no longer affects implementation or
  validation.
- Reuse findings; batch independent reads or checks. Sequence edits and dependent work.
  Read focused logs without hiding failures or exit status; keep full
  diagnostics only when useful.
- Plan when dependencies warrant it. For long tasks, keep a compact checkpoint
  of decisions, changed files, checks, and next action.
