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
- Use stable lazy-item identities and deliberate navigation-entry scope for
  state holders. Preserve back navigation, restoration, and deep links in
  navigation changes.
- Reuse theme tokens, resources, and components. Lay out by available
  constraints, not device labels or assumed sizes; account for text scaling,
  localization, input methods, and window insets.
- Provide accessible semantics, meaningful actions, focus behavior, and adequate
  interaction targets.
  Verify keyboard, pointer, and touch on relevant platforms; accessibility
  support differs by platform.
- Keep expensive work out of frequent recomposition. Memoize with correct keys
  when useful; use `derivedStateOf` when output changes less often than inputs.
  Add caching or stability annotations only with evidence and valid contracts.
- Check transitions and interactions beyond previews. Validate affected platform
  hosts alongside shared UI when platform behavior changes.
