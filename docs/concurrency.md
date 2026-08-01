# Prices concurrency and performance rules

Production code must protect the UI from deadlocks, ANRs, freezes, duplicate work, and unsafe coroutine ownership.

## Required practices

- Use structured concurrency and lifecycle-owned scopes such as `viewModelScope`.
- Represent screen state with immutable UI state and `StateFlow`.
- Collect state with `collectAsStateWithLifecycle`.
- Inject dispatchers where boundaries need testability; use `Dispatchers.IO` behind repository/data boundaries.
- Make network work cancellable and add appropriate timeouts.
- Debounce and cancel superseded product-search requests.
- Map expected exceptions into UI state.
- Never swallow `CancellationException`; rethrow it or let it propagate.
- Keep Activity and View references out of ViewModels and repositories.

## Prohibited in production paths

Do not use `runBlocking`, `Thread.sleep`, `Future.get`, blocking joins, main-thread network/filesystem/crypto/database work, unmanaged `GlobalScope`, unnecessary synchronization, or undocumented manual locks. Do not perform side effects during Compose recomposition or put blocking work inside Composables.

Do not create nested coroutine scopes without a clearly documented owner and cancellation policy.

## Search and mutation workflows

Search must be debounced, cancellable, and scoped to the active screen. A newer query must be able to supersede an older request without leaking work.

Price mutations must use single-flight behavior per product/variation and prevent duplicate submissions from repeated taps. Idempotency-Key generation belongs to the request workflow, with a clear policy for retries and replay.

Conflict responses must become non-blocking UI state. The user should be able to review the current server value, decide what to do, and continue without a blocked thread or frozen screen.
