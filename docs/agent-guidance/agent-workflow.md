# Agent workflow

- Scale investigation to complexity and risk. Use targeted filename/text searches
  and relevant sections; trace affected callers, configuration, and tests. Load
  only relevant guidance/docs; stop when evidence suffices to implement and validate.
- Bound search scope and tool output; narrow truncated results. Keep failures and
  exit status visible; retain full diagnostics when useful. Verify uncertain or
  version-sensitive APIs against primary docs for the project's versions.
- Reuse findings and in-context reads. Repeat tool calls only for changed state,
  missing context, or unresolved questions; repeat checks when relevant changes,
  failures, evidence, or project policy require it. Batch independent operations
  when useful; sequence edits and dependent work.
- Plan only when useful; keep plans and updates concise. For long tasks, maintain
  a checkpoint of the objective, constraints, decisions, changed files, checks,
  failed approaches, and next action. Update existing task state; reference
  evidence locations instead of copying logs or accumulating journals.
- For change reviews, inspect the actual diff and affected callers. Report
  actionable issues caused by the changes, including indirect breakage; exclude
  unrelated pre-existing issues unless requested. Verify findings against code
  or checks, merge duplicates, and give each a location, trigger, and impact.
  Report no findings when none qualify.
