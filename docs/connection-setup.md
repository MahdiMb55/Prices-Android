# Store discovery and connection setup

Discovery accepts a WordPress URL or a host-only value. Host-only input is explicitly treated as HTTPS; the app never falls back from HTTPS to HTTP. The value is normalized to the confirmed Prices endpoint namespace and discovery calls `GET /wp-json/prices/v1/discovery`.

Only a validated discovery response is persisted. Validation requires the confirmed `prices/v1` API, WooCommerce availability, pairing-code capability, required safe store metadata, and a supported app version. The typed `StoredConnection` snapshot has schema version 1 and phase `Discovered`; it excludes credentials, pairing codes, authorization headers, tokens, and raw JSON.

`minimum_app_version` uses numeric dotted comparison. A malformed server value is non-blocking, remains stored as the original value, and is exposed through `minimumAppVersionWarning` for diagnostics. It is never treated as an app-update requirement.

There is one cancellable discovery request per onboarding screen. A newer request cancels the old one and stale results cannot update UI or persistence. There are no retries and cancellation propagates unchanged.
