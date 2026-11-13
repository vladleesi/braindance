# Web frontend

- Prefer semantic HTML/native controls. Provide accessible names, visible focus,
  keyboard operation, meaningful heading order, and loading/empty/invalid/failure
  feedback.
- Lay out by available space and content; check text zoom, contrast, reduced motion,
  themes, and input methods. Preserve essential content/links without JavaScript
  where progressive enhancement is supported.
- Reuse color, typography, and spacing tokens across components, states, and themes.
- Clean up listeners, observers, timers, and obsolete requests; prevent duplicate
  subscriptions and stale responses overwriting new state. Isolate storage/network
  failures from unrelated interactions.
- Preserve history, reloadable deep links, and URL/base-path contracts; persist
  navigation information needed after reload.
- Measure production output before optimizing. Reserve media dimensions, size/
  compress assets, and defer noncritical work; avoid speculative preloading/caching.
  Use long-lived immutable caching only for content-fingerprinted static assets;
  keep HTML entry points revalidatable to discover updated asset URLs.
- For public indexable pages, keep titles, descriptions, canonicals, structured
  data, feeds, and sitemaps accurate and consistent with visible content.
