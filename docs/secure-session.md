# Secure session persistence

## Phase A token storage

The device token is encrypted with an AES-256 key held by AndroidKeyStore using `AES/GCM/NoPadding`. Each record has a version, randomized IV, and ciphertext. The secure record is stored in private `prices_secure_session` preferences and excluded from cloud backup and device transfer. The token is never part of UI state, routes, logs, or printable representations.

## Phase B paired-session persistence

After pairing exchange and `GET /auth/me` verification, the app temporarily holds the token in the in-memory access-token store. It then performs these operations in order:

1. Write the token through `SecureTokenStorage`.
2. Write `StoredPairedSessionMetadata` through the non-sensitive DataStore boundary.
3. Establish the final in-memory authenticated `PairedSession`.

Metadata is stored separately in `prices_paired_session_metadata`. It contains schema version, device identity, user identity, authentication method, token type, safe discovery capabilities, pairing time, and the API-base reference. It contains no token, pairing code, credential, raw DTO, or authorization header.

If any required step fails, the coordinator clears the in-memory token and session and rolls back persistent records. Rollback failure is classified separately from the original write failure. The discovered `StoredConnection` is preserved when pairing persistence fails.

Local Disconnect clears in-memory token/session first, then secure token and paired metadata. It preserves the discovered store and performs no server revocation. Change Store performs the same cleanup and then clears the discovered store before returning to onboarding.

Secure-session restoration is intentionally deferred. After process restart, the existing startup flow still sees the discovered store and routes to Pairing. It does not load metadata, load the secure token, verify `/auth/me`, or open Products. Startup restoration is deferred to Phase C.
