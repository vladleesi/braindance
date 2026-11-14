# Compose UI

- Render state and emit events; hoist state to the lowest owner needing control.
  Pass values/callbacks to reusable components rather than acquiring screen dependencies.
- Keep external side effects out of composable bodies. Key effects by restart
  inputs; dispose listeners/resources and use updated callbacks without restarting.
- Match state lifetime: `remember` for composition, saveable state for suitable
  recreation needs, scoped holders for screen/business work. Collect with lifecycle
  awareness where supported; deliberately scope navigation-entry holders. Preserve
  back navigation, restoration, and deep links.
- Reuse theme tokens (including spacing/shapes), resources, and components. Lay
  out by available constraints; account for text scaling, localization, input
  methods, and window insets rather than device labels/assumed sizes.
- Provide accessible semantics, actions, focus, and adequate interaction targets.
  Verify keyboard, pointer, and touch on relevant platforms; accessibility varies.
- Give the app background one owner, normally the root scaffold; add opaque screen
  backgrounds only for different colors. Remove backgrounds, `Surface` fills, and
  container colors duplicating parent fills. On Android, clear redundant window
  backgrounds with `window.setBackgroundDrawable(null)` after `setContent` only
  with opaque Compose root coverage through transitions and inset handling.
- Use lazy layouts for large/unbounded collections, stable unique `key`s, and
  `contentType`s matching reusable item structures; use ordinary layouts for small
  fixed groups. For expensive items, use `LazyLayoutCacheWindow` only where all
  affected targets support it; start about one viewport ahead/behind and tune
  against frame timings/memory. Isolate experimental APIs/opt-ins in one small helper.
- Prepare immutable UI models in state holders; sort, map, format, and parse outside
  composition. Hoist constant shapes, brushes, and collections or `remember` with
  correct keys; do not memoize compiler-handled lambdas. Honor stability contracts;
  never hide mutable state with stability annotations.
- Defer scroll/animation/pointer reads to layout/draw lambdas (`Modifier.offset { }`,
  `graphicsLayer { }`, `drawBehind { }`) when composition does not need them. Use
  derived state when composition decisions change less often than inputs:
  `derivedStateOf`, or `computedStateOf` for cheap derivations where supported.
- Give wrapper layouts a layout purpose. Constrain images before loading to decode
  at display size; reuse the project's loader/caching and change keys only with content.
- Prefer existing modifiers; implement custom behavior with `Modifier.Node` over
  `Modifier.composed`. Avoid unnecessary offscreen rendering in scrolling content
  (large translucent layers, blur, shadows, `CompositingStrategy.Offscreen`).
- In approved Android rendering investigations, use overdraw overlay, frame
  statistics, and traces. Aim for one or two total draws in ordinary content areas.
  Interleave repeated runs; separate UI-thread, RenderThread, and compositor waits
  before attributing missed frames. GPU rendering bars are not GPU execution time.
