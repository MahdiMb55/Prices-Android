# Prices Android architecture

## Product boundary

Prices is the official Android client for the custom Prices WordPress plugin. The app communicates with the custom REST namespace `/wp-json/prices/v1`; it is not a generic WooCommerce REST API client. The package name and application ID remain `com.mahdiMb55.prices`.

## MVP shape

Keep one `app` module. Use Kotlin and Jetpack Compose with a practical feature-oriented package structure:

```text
com.mahdiMb55.prices
├── app
├── core
│   ├── designsystem
│   ├── navigation
│   ├── appinfo
│   ├── di
│   ├── common
│   └── model
├── data
│   ├── remote
│   ├── local
│   ├── security
│   └── repository
└── feature
    ├── onboarding
    ├── pairing
    ├── products
    ├── productdetail
    ├── priceedit
    ├── pricehistory
    └── settings
```

Create directories only when real code needs them. Do not introduce multi-module architecture until the MVP has a demonstrated boundary that justifies it.

## Responsibilities and dependency direction

- `feature` owns screen-level UI, ViewModels, and user-facing state.
- `data` owns API, persistence, security integration, DTO mapping, and repository implementations.
- `core` owns reusable UI, navigation primitives, common utilities, and shared models.
- `app` owns application startup and dependency wiring.
- UI depends on stable repository interfaces or small use cases; UI does not depend on Retrofit DTOs.
- Data depends inward on shared models and contracts, never on feature UI.

Use a domain use case when it represents reusable business policy, coordinates multiple repositories, or makes a ViewModel substantially clearer. Keep simple one-repository reads and writes in the repository/ViewModel path until that policy exists; do not create use-case classes ceremonially.

## Dependency injection

The MVP uses explicit constructor injection backed by one small application-scoped `AppContainer`. `PricesApplication` owns the container through the narrow `AppContainerOwner` contract, and `PricesApp` provides it at the navigation boundary. Only route-level composables may consume the typed composition local; leaf composables receive immutable UI state and callbacks. Feature ViewModels receive their individual dependencies through feature-specific `ViewModelProvider.Factory` implementations and must not know that the container exists.

Hilt was evaluated, but the tested available Hilt Gradle plugins depend on the removed legacy Android `BaseExtension` integration and fail with the locked AGP 9.0.1 toolchain. AGP will not be downgraded solely to add Hilt. This explicit wiring is a small migration-friendly compatibility choice, not a permanent custom DI framework. Hilt may be reconsidered in a dedicated tooling-maintenance change after a compatible version is available and verified. See [ADR 0001](adr/0001-explicit-dependency-injection.md).

Do not add generic dependency maps, reflection, runtime registration, global mutable singletons, or service-locator access. Add bindings only when a real dependency is implemented. Dispatcher injection begins when asynchronous data work creates a genuine testability boundary; no dispatcher abstraction exists for synchronous app metadata.

Repositories expose suspend operations and/or `Flow`/`StateFlow`-friendly streams. ViewModels own screen state and transform repository results into immutable UI state. Activities and Views must not be retained by ViewModels or repositories.

## Networking foundation

`data.remote` owns the reusable transport boundary for the custom Prices API. Retrofit and OkHttp use Kotlin serialization with `ignoreUnknownKeys = true`; API DTOs remain transport-only and must be mapped before any future domain or UI use. The sole currently defined contract is discovery at `GET /wp-json/prices/v1/discovery`.

Store URLs are runtime input, never a global production base URL. `StoreUrlNormalizer` accepts only `http` or `https` URLs with a host, rejects credentials, queries, fragments, malformed input, and paths after the API namespace, and produces a canonical `.../wp-json/prices/v1/` base while preserving a WordPress subdirectory and non-default port. HTTPS is the production default; release cleartext remains disabled and local HTTP use is limited to JVM MockWebServer tests.

`PricesApiFactory` creates a client only from a validated base URL, uses explicit finite timeouts, and does not perform work during construction. `AppContainer` exposes that factory rather than an active-store client. A synchronous in-memory `AccessTokenProvider` is the only authentication foundation: interceptors may add a non-blank bearer token but must never read disk, DataStore, keystore, or suspend state. Future pairing updates the in-memory source outside interception.

Network failures preserve HTTP status and stable backend error codes without parsing human-readable messages. `NetworkRequestExecutor` rethrows cancellation, does not retry, and maps connectivity, timeout, TLS, HTTP, authentication, permission, validation, conflict, server, serialization, and unknown failures into `NetworkResult`. HTTP logging is not installed; authorization headers, pairing codes, tokens, and response bodies must never be logged.

## Planned product capabilities

The MVP will grow toward pairing, product search, product and variation price editing, conflict handling, and price-history display. Pairing and authentication details must use the Prices plugin contract, not assumptions from the generic WooCommerce API.

## Discovery startup rule

The app asynchronously reads one non-sensitive DataStore snapshot at startup. An absent, corrupted, or unsupported snapshot starts onboarding. A valid `Discovered` snapshot starts the pairing destination, which displays only safe store metadata and explicitly does not imply authentication. Products and authenticated startup states do not exist yet.

The snapshot has an explicit schema version and contains no tokens, pairing codes, credentials, headers, or raw discovery JSON. Change Store clears only this discovered snapshot. Real pairing and secure storage remain future work.

## Testing strategy

- Unit-test price rules, DTO mapping, conflict decisions, and ViewModel state transitions.
- Test repositories with fake or mock API/local boundaries.
- Test Compose behavior and semantics, not pixel-perfect implementation details.
- Cover English LTR and Persian RTL for important flows.
- Add connected tests for startup and critical flows when the emulator is reliable.

## Tooling and platform constraints

Preserve the current package/application ID, minSdk 26, compileSdk 36, targetSdk 36, Kotlin 2.0.21, AGP 9.0.1, Gradle 9.1.0, and Compose BOM 2024.09.00 unless explicitly approved otherwise. The current build requires JDK 17 or newer; JDK 22 is the verified local development JDK.

The Compose BOM remains `2024.09.00`, while Compose instrumentation-test artifacts are explicitly pinned to `1.9.2` to keep the current suite executable. A future dedicated tooling-maintenance change should align the Compose dependency set consistently; feature commits must not adjust this compatibility pin opportunistically.

## Commit strategy

Keep commits small and reviewable. Separate documentation, design system, infrastructure, and product behavior. Before each commit, run the relevant tests and `git diff --check`; verify that local properties, generated output, IDE user state, signing material, credentials, tokens, and secrets are absent.
