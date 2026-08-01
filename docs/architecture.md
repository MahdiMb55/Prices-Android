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

Repositories expose suspend operations and/or `Flow`/`StateFlow`-friendly streams. ViewModels own screen state and transform repository results into immutable UI state. Activities and Views must not be retained by ViewModels or repositories.

## Planned product capabilities

The MVP will grow toward pairing, product search, product and variation price editing, conflict handling, and price-history display. Pairing and authentication details must use the Prices plugin contract, not assumptions from the generic WooCommerce API.

## Current startup rule

The navigation shell starts at onboarding on every launch. This is an explicit temporary rule; the real startup decision will later depend on secure connection storage and must not be represented by fake authentication or persistent connection state in the shell.

## Testing strategy

- Unit-test price rules, DTO mapping, conflict decisions, and ViewModel state transitions.
- Test repositories with fake or mock API/local boundaries.
- Test Compose behavior and semantics, not pixel-perfect implementation details.
- Cover English LTR and Persian RTL for important flows.
- Add connected tests for startup and critical flows when the emulator is reliable.

## Tooling and platform constraints

Preserve the current package/application ID, minSdk 26, compileSdk 36, targetSdk 36, Kotlin 2.0.21, AGP 9.0.1, Gradle 9.1.0, and Compose BOM 2024.09.00 unless explicitly approved otherwise. The current build requires JDK 17 or newer; JDK 22 is the verified local development JDK.

## Commit strategy

Keep commits small and reviewable. Separate documentation, design system, infrastructure, and product behavior. Before each commit, run the relevant tests and `git diff --check`; verify that local properties, generated output, IDE user state, signing material, credentials, tokens, and secrets are absent.
