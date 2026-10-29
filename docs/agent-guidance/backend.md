# Backend services

- Define request/response contracts, deliberately reject unsupported inputs, and
  map failures to stable protocol errors without sensitive details.
- Bound requests, upstream calls, queues, and connections with timeouts and
  resource limits. Propagate cancellation; close clients, sessions, and
  background tasks through the application lifecycle.
- Retry only appropriate transient failures with bounded backoff. Make mutations
  idempotent where retries or duplicate delivery are possible; order
  acknowledgement and persistence to prevent silent data loss.
- Enforce cross-write invariants with transactions and database constraints.
  Handle concurrent updates; process-local locks cannot coordinate replicas. Use
  deterministic ordering and pagination for growing collections.
- Distinguish UTC instants from local dates/times at storage and API boundaries.
  Define precision and ordering rather than relying on driver defaults.
- Evolve schemas and APIs under the actual compatibility contract. Prefer
  staged, backward-compatible migrations when deployments coexist; protect
  existing data and verify recovery before destructive transitions.
- Keep caches, rate limits, and session validity correct across processes and
  replicas. Key caches by all representation and security variants; prevent
  shared caches from exposing private responses.
- Keep liveness and readiness checks inexpensive and suited to their purpose.
  Measure service-boundary latency and errors without verbose production logs.
