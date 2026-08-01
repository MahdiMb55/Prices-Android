# Prices API networking

## API boundary

The Android client communicates only with the custom Prices WordPress namespace:

```text
/wp-json/prices/v1
```

It is not a generic WooCommerce REST client. Android never stores or sends WooCommerce consumer keys or secrets. A device bearer token will be issued by a future pairing flow; pairing is not implemented by this networking foundation.

The only implemented Retrofit contract is discovery:

```text
GET /wp-json/prices/v1/discovery
```

The response is represented by transport DTOs only. Discovery fields such as plugin/API versions, availability, authentication capabilities, feature keys, site metadata, and currency are not UI models. Unknown JSON fields are tolerated so compatible server additions do not break decoding; required known fields still fail decoding when absent.

## Runtime base URLs

Stores are user-provided at a future onboarding boundary. `StoreUrlNormalizer` returns either a typed validated base URL or a typed validation error. It accepts only `http` and `https` URLs with a host, preserves non-default ports and WordPress subdirectories, removes trailing slashes, and appends the namespace exactly once.

Examples:

```text
https://example.com                 -> https://example.com/wp-json/prices/v1/
https://example.com/shop/           -> https://example.com/shop/wp-json/prices/v1/
https://example.com/shop/wp-json/prices/v1
                                    -> https://example.com/shop/wp-json/prices/v1/
```

Malformed URLs, unsupported schemes, missing hosts, embedded credentials, queries, fragments, and paths after `/wp-json/prices/v1` are rejected. The normalizer does not invent a valid-looking URL from invalid input. HTTPS is preferred; `http` remains accepted only so local/non-production endpoints can be represented explicitly.

## Clients, authentication, and safety

`PricesApiFactory` receives a validated base URL and creates a dedicated Retrofit/OkHttp client. There is no global mutable Retrofit instance, no active-store singleton, and no client created at application startup. Connection, read, write, and call timeouts are explicit and finite.

`AuthHeaderInterceptor` reads a synchronous `AccessTokenProvider`. It adds `Authorization: Bearer <token>` only for non-blank tokens and preserves a caller-provided Authorization header. The foundation uses an in-memory provider only. Interceptors never read keystore, DataStore, disk, crypto, or suspend state, and never use `runBlocking`.

TLS verification and hostname verification use platform defaults. The app has no trust-all certificates, hostname bypasses, or release cleartext allowance. MockWebServer covers local JVM HTTP tests without changing Android’s production cleartext policy. No HTTP logging interceptor is installed; authorization values, pairing codes, tokens, and response bodies are never logged.

## Errors and cancellation

The backend’s machine-readable `error.code` is preserved exactly, including values such as `AUTHENTICATION_REQUIRED`, `PERMISSION_DENIED`, `PRICE_CONFLICT`, and `INVALID_FILTER`. Human-readable server messages are presentation input only and must not control behavior. `NetworkError` retains status, code, optional validation details, request ID, category, and retryability without exposing Retrofit or OkHttp objects to UI.

`NetworkRequestExecutor` is suspend, makes no retries, and rethrows `CancellationException`. It classifies response failures and transport exceptions without blocking or moving Retrofit suspend calls to another dispatcher.
