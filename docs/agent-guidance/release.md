# Versioning and releases

- Where SemVer 2.0.0 is adopted, classify the declared public API from `1.0.0`:
  MAJOR for incompatible changes, MINOR for compatible additions/deprecations,
  PATCH for compatible fixes. Reset lower components on MAJOR/MINOR increments;
  `0.y.z` offers no stable API guarantee. Document user-visible changes and migrations.
- For SemVer pre-releases where used, `-alpha.1` precedes its normal release;
  numeric identifiers have no leading zeroes; `+build` metadata does not affect
  precedence. Do not introduce pre-release conventions where absent.
