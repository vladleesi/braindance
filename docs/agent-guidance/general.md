# General engineering

- Record lasting decisions in existing instructions/docs; create no files for
  trivial preferences.
- Name parameters/properties by content (`snapshotJson`, `gameId`); document
  sources or formats the name does not convey. Use clear control flow; explain
  non-obvious constraints rather than syntax.
- Separate business decisions from independently changing presentation, transport,
  and persistence. Pass dependencies explicitly; add layers/interfaces only for
  required boundaries or substitutions.
- Reuse suitable code, standard libraries, and platform APIs. Add or suggest tools,
  dependencies, abstractions, services, or processes only when current needs or
  concrete risks justify them; choose the simplest adequate solution. Do not repeat
  unrelated suggestions.
- Model meaningful states and failures; distinguish absent, empty, invalid, and
  failed results where needed. Never silently replace failures with successful
  defaults or discard error context.
- Give resources an owner and cleanup on success, failure, and cancellation;
  define ordering for shared mutable state and overlapping work.
- Optimize measured bottlenecks with representative workloads. Define cache
  lifetime, keys, and invalidation; avoid speculative complexity.
- Write docs and published-site copy for usage, integration, operation, maintenance,
  and contribution. Explain each topic once and link to it; keep README to purpose,
  quick start, and references, with detail in existing docs. Preserve useful links,
  provenance, and rationale that prevents mistakes.
- Replace stale or repetitive explanations within scope. Use short paragraphs
  and minimal working examples. Use descriptive headings, lists, tables, and
  emphasis where they help readers find or use information. Avoid arbitrary size
  targets. Omit task histories, progress, abandoned approaches, speculation,
  promotion, and private context without reader value.
- When writing or editing docs or other project prose, including UI copy and
  messages, match the audience and established voice. Lead with what the reader
  needs. Replace buzzwords, hype, and vague benefits with supported details about
  what changes or what the reader can do. State uncertainty when it affects the
  reader's decision. Keep natural variation in tone and describe relevant technical
  trade-offs. Avoid overly polished or uniformly positive language. Never invent
  personal experiences or add unsupported claims to make writing sound authentic.
- Cut stock openers, filler transitions, artificial contrasts, explanations of
  the obvious, and summaries or conclusions that repeat the text. Let the content
  guide sentence and paragraph length, list size, and section structure. Vary
  repeated sentence openings and avoid forced groups of three or recurring
  challenge-solution-impact templates. Use em dashes sparingly. In casual prose,
  prefer a period or conjunction to a semicolon when it reads more clearly.
  Use these patterns as editing cues. Authorship requires evidence beyond style.
- Update affected docs and published content with changes to setup, usage,
  contracts, limitations, descriptions, examples, versions, and compatibility.
  Reuse site architecture, styling, generator, and validation; add no unrelated site.
