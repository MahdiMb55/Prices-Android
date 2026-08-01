# Prices Android agent instructions

- Read `docs/architecture.md`, `docs/ui-guidelines.md`, and `docs/concurrency.md` before changing the project.
- Preserve package name and application ID `com.mahdiMb55.prices`.
- Keep the MVP in one app module unless a foundational change is explicitly approved.
- Do not upgrade Gradle, Kotlin, AGP, SDK values, dependencies, or the Compose BOM without explicit approval.
- Explain why any new dependency is needed before adding it.
- Keep commits focused and reviewable; run tests and `git diff --check` before committing.
- Never commit `local.properties`, build outputs, IDE user state, signing keys, credentials, tokens, or secrets.
- Never use blocking APIs in production paths: no `runBlocking`, `Thread.sleep`, blocking joins, main-thread I/O, unmanaged `GlobalScope`, or unsafe locks.
- Use structured concurrency, lifecycle-aware state collection, immutable UI state, and cancellation-aware work.
- Ask before foundational architecture changes or creating new layers without real behavior.
- The API boundary is the custom Prices WordPress namespace `/wp-json/prices/v1`, not a generic WooCommerce REST client.
- Do not copy Telegram branding, assets, icons, or proprietary screens. Use Persian Green `#00A693` as the primary brand source.
- Preserve Persian RTL and English LTR behavior, including correct direction for prices, SKUs, URLs, and identifiers.
