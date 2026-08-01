# ADR 0001: Explicit dependency injection for the MVP

## Status

Accepted

## Context

Prices needs application-scoped dependency ownership and constructor-injected ViewModels while preserving Android Gradle Plugin 9.0.1 and the rest of the locked build stack. Hilt was evaluated for this role. The currently available tested Hilt Gradle plugins fail with AGP 9.0.1 because their plugin integration requires the removed legacy Android DSL `BaseExtension` API.

Downgrading AGP solely to adopt Hilt would make the dependency-injection choice drive the project toolchain and is outside the approved maintenance scope.

## Decision

The MVP uses explicit constructor injection and one small application-scoped `AppContainer`:

- `PricesApplication` owns the container through `AppContainerOwner`.
- `DefaultAppContainer` creates only dependencies that exist now.
- A typed composition local carries the container across the application/navigation boundary.
- Only route boundaries resolve dependencies from that container.
- Feature ViewModels receive individual dependencies through feature-specific explicit factories.
- Stateless screens receive immutable UI state and callbacks.

The container will not provide generic lookup, reflection, mutable registration, runtime modules, or service-locator access. No dispatcher abstraction is introduced until asynchronous production work requires one.

## Consequences

Application startup remains lightweight and dependency ownership is explicit. Tests can supply narrow fakes without Android context or generated test infrastructure. There is some manual factory and wiring code, which is acceptable for the current single-module MVP while the dependency graph is small.

This is a migration-friendly compatibility decision, not a permanent custom framework. Hilt may be reconsidered in a dedicated tooling-maintenance change when an AGP 9-compatible version is available and verified. The migration maps directly:

- `AppContainer` binding → Hilt module/provider
- Explicit ViewModel factory → `@HiltViewModel`
- Application owner → `@HiltAndroidApp`

No Hilt annotations or dormant Hilt configuration are kept in the project.

## Related maintenance note

The Compose BOM remains `2024.09.00`, and instrumentation-test artifacts explicitly use `1.9.2`. Compose alignment belongs in a future dedicated maintenance change rather than this architecture increment.
