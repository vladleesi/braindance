# Compose UI

- Keep composables focused on rendering state and emitting events. Hoist state
  to the lowest owner that needs control; pass values and callbacks to reusable
  components instead of acquiring screen dependencies internally.
- Keep external side effects out of composable bodies. Key effect APIs by values
  that should restart work. Dispose listeners and resources; use updated
  callback references to observe changes without restarting effects.
- Match state lifetime: `remember` for composition-local values, saveable state
  for suitable recreation needs, scoped state holders for screen/business work.
  Collect with lifecycle awareness where supported.
- Use deliberate navigation-entry scope for state holders. Preserve back
  navigation, restoration, and deep links in navigation changes.
- Reuse theme tokens, including spacing and shapes, plus resources and
  components instead of repeating inline values. Lay out by available
  constraints, not device labels or assumed sizes; account for text scaling,
  localization, input methods, and window insets.
- Provide accessible semantics, meaningful actions, focus behavior, and adequate
  interaction targets.
  Verify keyboard, pointer, and touch on relevant platforms; accessibility
  support differs by platform.
- Give the app background one owner, normally the root scaffold. Add an opaque
  screen background only when its color differs; remove backgrounds, `Surface`
  fills, and container colors that repeat a parent fill. On Android, remove a
  redundant window background with `window.setBackgroundDrawable(null)` after
  `setContent` only when the Compose root covers the window opaquely, including
  during transitions and inset handling.
- Use lazy layouts for large or unbounded scrollable collections, with stable,
  unique item `key`s and `contentType` values matching reusable item structures.
  Keep small fixed groups in ordinary layouts. For expensive items, prefetch
  with `LazyLayoutCacheWindow` only where every affected target supports it:
  start about one viewport ahead and behind, then tune against frame timings
  and memory. Keep experimental APIs and their opt-ins in one small helper.
- Keep sorting, mapping, formatting, and parsing out of composition; prepare
  immutable UI models in a state holder. Hoist constant shapes, brushes, and
  collections or `remember` them with correct keys; do not manually memoize
  lambdas the compiler already handles. Use types with valid stability
  contracts; never add stability annotations to hide mutable state.
- Defer reads of scroll offsets, animation values, and pointer positions to
  layout or draw lambdas such as `Modifier.offset { }`, `graphicsLayer { }`, or
  `drawBehind { }` when composition does not need them. Use `derivedStateOf` for
  composition decisions whose output changes less often than their inputs.
- Give each wrapper layout a layout purpose. Constrain image bounds before
  loading so images decode at display size. Use the project's existing image
  loader and caching, with cache keys that change only when content changes.
- Implement custom modifier behavior with `Modifier.Node` rather than
  `Modifier.composed`; prefer existing modifiers when sufficient. Avoid
  unnecessary offscreen rendering on scrolling content, such as large layers
  with alpha below one, blur, shadows, or `CompositingStrategy.Offscreen`.
- In approved Android rendering investigations, use the GPU overdraw overlay,
  frame statistics, and traces. Aim for one or two total draws in ordinary
  content areas. Repeat interleaved runs, and separate UI-thread, RenderThread,
  and compositor waits before attributing missed frames; do not treat GPU
  rendering bars as GPU execution time.
