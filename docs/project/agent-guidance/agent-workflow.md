# Agent workflow

- Scale investigation to task complexity and risk. Start with targeted filename
  and text searches (`rg --files`, `rg`); read relevant files or sections, not
  entire directories. Trace affected callers, configuration, and tests as needed.
- Bound search scope and tool output; narrow truncated results. Read focused logs
  without hiding failures or exit status; retain full diagnostics when useful.
- Load only relevant guidance and docs. Verify uncertain or version-sensitive APIs
  against primary documentation for the project's versions. Stop exploring when
  evidence suffices for implementation and validation.
- Reuse established findings and in-context reads; repeat reads, searches, or tool
  calls only for changed state, missing context, or unresolved questions. Batch
  independent operations when useful; sequence edits and dependent work. Repeat
  checks only when relevant changes, failures, or evidence invalidate results.
- Keep plans and progress updates concise. Plan only when useful;
  for long tasks, checkpoint decisions, changed files, checks, and the next action.
