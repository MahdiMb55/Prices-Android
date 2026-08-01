# Manual device pairing

The Android app exchanges a one-time code through `POST /wp-json/prices/v1/pairing/exchange`. The request has `pairing_code`, `device_name`, `device_identifier` (`prices-android`), `app_version`, and `android_version`. Codes use the confirmed `0000-0000` format. The response provides `device_id`, `token_type` (`Bearer`), `device_token`, and `authentication_method` (`device_token`). Unknown response fields are tolerated.

The device token is retained only in application memory. It is never persisted, rendered, added to UI state, passed in routes, or logged. The app verifies the exchange with `GET /wp-json/prices/v1/auth/me`; a verification failure clears both the token and in-memory session.

The persisted DataStore record remains `Discovered`. Process restart returns to manual pairing because neither paired nor authenticated state is persisted. Disconnect clears only the in-memory session; it does not revoke a server device. QR pairing and secure token storage are deferred.

The backend returns `PAIRING_FAILED` for invalid, expired, and replayed codes, so the client presents one truthful combined message. `RATE_LIMITED`, `PAIRING_DISABLED`, `DEVICE_LIMIT_REACHED`, `PERMISSION_DENIED`, `AUTHENTICATION_REQUIRED`, and the common error envelope are mapped without using human-readable messages for logic.
