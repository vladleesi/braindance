# General engineering

- Record useful lasting decisions in existing instructions or relevant docs; do
  not create config or documents for trivial preferences.
- Prefer explicit names and clear control flow over dense expressions. Explain
  non-obvious constraints, not syntax.
- Separate business decisions from independently changing presentation,
  transport, and persistence. Pass dependencies explicitly; add interfaces or
  layers only for required boundaries or substitutions.
- Reuse suitable code, standard libraries, and platform APIs. Suggest or add
  tools, dependencies, abstractions, services, or processes only for current
  requirements or concrete risks with meaningful benefit. Avoid repeated
  unrelated suggestions; choose the simplest adequate solution.
- Model meaningful states and failures explicitly; distinguish absent, empty,
  invalid, and failed results where callers need it. Never silently turn
  failures into successful defaults or discard error context.
- Give resources an owner and cleanup across success, failure, and cancellation.
  Define ordering for shared mutable state and overlapping work.
- Optimize measured bottlenecks with representative workloads. Define cache
  lifetime, keys, and invalidation; avoid speculative complexity.
- Write new and updated documentation, including published-site copy, for its
  intended reader. Include what readers need to use, integrate, operate, maintain,
  or contribute to the project.
- Keep one authoritative explanation per topic and link to it elsewhere. Keep
  README focused on purpose, quick start, and reference links; put advanced details
  in existing docs first. Preserve useful external links and provenance.
- Replace stale explanations and remove repetition in the affected topic instead
  of continually appending sections. Keep cleanup scoped to the task; judge
  usefulness and clarity without arbitrary word limits or reduction percentages.
- Prefer short paragraphs, descriptive headings, and minimal working examples;
  use tables only when they make information easier to understand.
- Exclude task histories, progress reports, abandoned approaches, repetitive
  implementation summaries, and speculative guidance. Keep rationale only when
  it helps future readers avoid a meaningful mistake. Exclude unrelated promotion,
  personal infrastructure, and private development context without reader value.
- Update affected website or documentation sources alongside repository changes:
  setup, usage, contracts, limitations, descriptions, examples, versions, and
  compatibility. Check for published content; reuse site architecture, styling,
  generator, and validation. Add no unrelated site.
