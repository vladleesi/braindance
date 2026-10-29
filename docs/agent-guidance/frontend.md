# Web frontend

- Use semantic HTML and native controls before recreating browser behavior.
  Provide accessible names, visible focus, keyboard operation, meaningful
  heading order, and loading, empty, invalid, and failure feedback.
- Lay out responsively by available space and content. Check text zoom,
  contrast, reduced motion, and relevant themes and input methods. Preserve
  essential content and links without JavaScript where the product supports
  progressive enhancement.
- Reuse color, typography, and spacing tokens; keep themed visuals consistent
  across components and states.
- Clean up listeners, observers, timers, and obsolete requests. Prevent
  duplicate subscriptions and stale responses overwriting new state. Handle
  unavailable storage and network failures without breaking unrelated
  interactions.
- Preserve history, reloadable deep links, and URL/base-path behavior as public
  contracts. Do not keep navigation information needed after reload only in memory.
- Measure production output before optimizing. Reserve media dimensions, size
  and compress assets appropriately, and defer noncritical work. Avoid
  speculative preloading or caching. Match cache policy to asset identity: use
  long-lived immutable caching only for content-fingerprinted static assets;
  keep HTML entry points revalidatable so clients can discover updated asset URLs.
- For public indexable pages, keep titles, descriptions, canonicals, structured
  data, feeds, and sitemaps accurate and consistent with visible content.
  Validate output and links; never invent facts or freshness signals.
